import { access, mkdir, readdir, readFile, writeFile } from "node:fs/promises";
import { constants, existsSync } from "node:fs";
import path from "node:path";
import process from "node:process";
import { spawn } from "node:child_process";
const sessions = new Map();
const AGENT_PROMPT_MAX_BYTES = 64 * 1024;
const ALLOWED_AGENT_IDS = new Set([
  "product-manager",
  "ui-designer",
  "architect",
  "frontend-engineer",
  "backend-engineer",
  "test-engineer",
  "ops-engineer"
]);

function repoRoot() {
  if (process.env.REPO_ROOT) {
    return process.env.REPO_ROOT;
  }

  let current = process.cwd();
  while (current !== path.dirname(current)) {
    if (existsSync(path.join(current, "CLAUDE.md")) && existsSync(path.join(current, ".claude", "skills"))) {
      return current;
    }
    current = path.dirname(current);
  }

  return path.resolve(process.cwd(), "..", "..");
}

function defaultWorkspaceRoot() {
  return path.resolve(repoRoot(), "workspace", "runtime");
}

function resolveWorkspaceRoot(workspaceRoot = defaultWorkspaceRoot()) {
  if (!workspaceRoot || workspaceRoot.trim().length === 0) {
    return defaultWorkspaceRoot();
  }

  return path.isAbsolute(workspaceRoot) ? workspaceRoot : path.resolve(repoRoot(), workspaceRoot);
}

function defaultDocsRoot() {
  return path.resolve(repoRoot(), "docs");
}

function defaultGeneratedRoot() {
  return path.resolve(repoRoot(), "generated");
}

async function ensureDirectory(dirPath) {
  await mkdir(dirPath, { recursive: true });
}

function stripFrontmatter(content) {
  if (!content.startsWith("---")) {
    return content.trim();
  }

  const endIndex = content.indexOf("\n---", 3);
  if (endIndex < 0) {
    return content.trim();
  }

  return content.slice(endIndex + 4).trim();
}

async function loadAgentPrompt(agentId) {
  if (!agentId || !ALLOWED_AGENT_IDS.has(agentId)) {
    return { prompt: "", agentId: "" };
  }

  const agentsDir = path.resolve(repoRoot(), ".claude", "agents");
  const agentPath = path.resolve(agentsDir, `${agentId}.md`);
  const relativePath = path.relative(agentsDir, agentPath);
  if (relativePath.startsWith("..") || path.isAbsolute(relativePath)) {
    return { prompt: "", agentId: "" };
  }

  try {
    const content = await readFile(agentPath, "utf8");
    if (Buffer.byteLength(content, "utf8") > AGENT_PROMPT_MAX_BYTES) {
      return { prompt: "", agentId: "" };
    }
    return { prompt: stripFrontmatter(content), agentId };
  } catch {
    return { prompt: "", agentId: "" };
  }
}

function mergeSystemPrompt(agentPrompt, modePrompt) {
  if (!agentPrompt) {
    return modePrompt;
  }

  return `${agentPrompt}\n\n--- Runtime Mode Instructions ---\n${modePrompt}`;
}

function normalizeProjectName(value) {
  const sanitized = (value || "")
    .replace(/[\\/:*?"<>|]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  return sanitized || "未命名项目";
}

function javaStringHash(value) {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = Math.imul(31, hash) + value.charCodeAt(index);
    hash |= 0;
  }
  return hash === -2147483648 ? 2147483648 : Math.abs(hash);
}

function deriveGeneratedProjectName(projectName) {
  const aliases = new Map([
    ["移动端应用：会议室预约系统", "HX-Meeting"],
    ["HX-Meeting 会议室预约系统", "HX-Meeting"],
    ["会议室预约系统", "HX-Meeting"]
  ]);
  const normalized = normalizeProjectName(projectName || "");
  if (aliases.has(normalized)) {
    return aliases.get(normalized);
  }
  const ascii = normalized
    .replace(/[^a-zA-Z0-9\s_-]/g, " ")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
  return ascii || `Project-${javaStringHash(normalized).toString(36)}`;
}

function deriveProjectName(requirement) {
  const text = (requirement || "").replace(/\s+/g, " ").trim();
  if (!text) {
    return "未命名项目";
  }

  const lines = text
    .split(/[\n。！？!?]/)
    .map((line) => line.trim())
    .filter(Boolean);
  const firstLine = lines[0] || text;
  const candidates = [
    /(?:项目名称|产品名称|需求名称|主题)[：: ]+([^，。,；;（）()\n]+)/,
    /做一个([^，。,；;（）()\n]{2,30})/,
    /开发一个([^，。,；;（）()\n]{2,30})/,
    /创建一个([^，。,；;（）()\n]{2,30})/,
    /搭建一个([^，。,；;（）()\n]{2,30})/
  ];

  for (const candidate of candidates) {
    const matched = text.match(candidate);
    if (matched?.[1]) {
      return normalizeProjectName(matched[1]);
    }
  }

  return normalizeProjectName(firstLine.slice(0, 30));
}

function artifactPaths(projectRoot) {
  const projectDesignDir = path.join(projectRoot, "产品设计");
  const uiPrototypeDir = path.join(projectRoot, "UI原型");
  const architectureDir = path.join(projectRoot, "架构设计");
  const databaseDir = path.join(projectRoot, "数据库设计");
  const testDir = path.join(projectRoot, "测试");
  const miscDocsDir = path.join(projectRoot, "其他文档");

  return {
    projectDesignDir,
    uiPrototypeDir,
    architectureDir,
    databaseDir,
    testDir,
    miscDocsDir,
    changeLogDir: path.join(projectDesignDir, "01-变更记录"),
    requirementDir: path.join(projectDesignDir, "02-产品需求文档"),
    uiDir: path.join(projectDesignDir, "03-UI设计规范"),
    flowDir: path.join(projectDesignDir, "04-流程图"),
    appendixDir: path.join(projectDesignDir, "05-附录"),
    uiPrototypeDesignDir: path.join(uiPrototypeDir, "设计稿"),
    uiPrototypeComponentDir: path.join(uiPrototypeDir, "组件库"),
    uiPrototypeInteractionDir: path.join(uiPrototypeDir, "交互原型"),
    uiPrototypeResponsiveDir: path.join(uiPrototypeDir, "移动端适配"),
    architectureSystemDir: path.join(architectureDir, "01-系统架构"),
    architectureApiDir: path.join(architectureDir, "02-接口设计"),
    architectureApiDefinitionsDir: path.join(architectureDir, "02-接口设计", "接口定义"),
    architectureDeployDir: path.join(architectureDir, "03-部署架构"),
    architectureTechDir: path.join(architectureDir, "04-技术选型"),
    databaseDictionaryDir: path.join(databaseDir, "数据字典"),
    databaseDDLDir: path.join(databaseDir, "建表脚本"),
    databaseMigrationDir: path.join(databaseDir, "数据迁移脚本"),
    testCasesDir: path.join(testDir, "测试用例"),
    testReportsDir: path.join(testDir, "测试报告"),
    meetingMinutesDir: path.join(miscDocsDir, "会议纪要")
  };
}

async function ensureWorkspace(taskId, requirement = "", workspaceRoot = defaultWorkspaceRoot(), docsRoot = defaultDocsRoot()) {
  const resolvedWorkspaceRoot = resolveWorkspaceRoot(workspaceRoot);
  const taskRoot = path.join(resolvedWorkspaceRoot, taskId);
  const logsDir = path.join(taskRoot, "logs");
  const contextDir = path.join(taskRoot, "context");
  const projectName = deriveProjectName(requirement);
  const generatedProjectName = deriveGeneratedProjectName(projectName);
  const generatedProjectRoot = path.join(defaultGeneratedRoot(), generatedProjectName);
  const projectDir = generatedProjectRoot;
  const frontendDir = path.join(generatedProjectRoot, "frontend");
  const backendDir = path.join(generatedProjectRoot, "backend");
  const testsDir = path.join(generatedProjectRoot, "tests");
  const projectRoot = path.join(docsRoot, projectName);
  const artifactDir = artifactPaths(projectRoot);

  await Promise.all([
    ensureDirectory(taskRoot),
    ensureDirectory(projectDir),
    ensureDirectory(frontendDir),
    ensureDirectory(backendDir),
    ensureDirectory(testsDir),
    ensureDirectory(logsDir),
    ensureDirectory(contextDir),
    ensureDirectory(projectRoot),
    ensureDirectory(artifactDir.projectDesignDir),
    ensureDirectory(artifactDir.uiPrototypeDir),
    ensureDirectory(artifactDir.architectureDir),
    ensureDirectory(artifactDir.databaseDir),
    ensureDirectory(artifactDir.testDir),
    ensureDirectory(artifactDir.miscDocsDir),
    ensureDirectory(artifactDir.changeLogDir),
    ensureDirectory(artifactDir.requirementDir),
    ensureDirectory(artifactDir.uiDir),
    ensureDirectory(artifactDir.flowDir),
    ensureDirectory(artifactDir.appendixDir),
    ensureDirectory(artifactDir.uiPrototypeDesignDir),
    ensureDirectory(artifactDir.uiPrototypeComponentDir),
    ensureDirectory(artifactDir.uiPrototypeInteractionDir),
    ensureDirectory(artifactDir.uiPrototypeResponsiveDir),
    ensureDirectory(artifactDir.architectureSystemDir),
    ensureDirectory(artifactDir.architectureApiDir),
    ensureDirectory(artifactDir.architectureApiDefinitionsDir),
    ensureDirectory(artifactDir.architectureDeployDir),
    ensureDirectory(artifactDir.architectureTechDir),
    ensureDirectory(artifactDir.databaseDictionaryDir),
    ensureDirectory(artifactDir.databaseDDLDir),
    ensureDirectory(artifactDir.databaseMigrationDir),
    ensureDirectory(artifactDir.testCasesDir),
    ensureDirectory(artifactDir.testReportsDir),
    ensureDirectory(artifactDir.meetingMinutesDir)
  ]);

  const metadataPath = path.join(taskRoot, "metadata.json");
  try {
    await access(metadataPath, constants.F_OK);
  } catch {
    await writeFile(metadataPath, JSON.stringify({ taskId, projectName, generatedProjectName, generatedProjectRoot, createdAt: new Date().toISOString() }, null, 2));
  }

  return { taskRoot, projectDir, frontendDir, backendDir, testsDir, generatedProjectName, generatedProjectRoot, logsDir, contextDir, metadataPath, projectName, projectRoot, projectDesignDir: artifactDir.projectDesignDir, artifactDir };
}

function createSessionId(taskId) {
  return `${taskId}-${Date.now()}`;
}

async function persistJson(contextDir, fileName, content) {
  const target = path.join(contextDir, fileName);
  await writeFile(target, JSON.stringify(content, null, 2));
  return target;
}

async function loadPersistedSession(taskId, workspaceRoot = defaultWorkspaceRoot()) {
  if (!taskId) {
    return null;
  }

  const resolvedWorkspaceRoot = resolveWorkspaceRoot(workspaceRoot);
  const sessionPath = path.join(resolvedWorkspaceRoot, taskId, "context", "session.json");
  try {
    const content = await readFile(sessionPath, "utf8");
    const session = JSON.parse(content);
    if (session?.taskId !== taskId || !session?.sessionId) {
      return null;
    }
    sessions.set(taskId, session);
    return session;
  } catch {
    return null;
  }
}

async function checkCliInstalled() {
  try {
    await runProcess("claude", ["--version"]);
    return true;
  } catch {
    return false;
  }
}

async function detectLoginStatus() {
  try {
    const { stdout } = await runProcess("claude", ["auth", "status"]);
    const status = JSON.parse(stdout);
    return Boolean(status.loggedIn);
  } catch {
    return false;
  }
}

async function detectAvailableSkills() {
  const skillsDir = path.join(repoRoot(), ".claude", "skills");
  try {
    const entries = await readdir(skillsDir, { withFileTypes: true });
    const names = await Promise.all(entries
      .filter((entry) => entry.isDirectory())
      .map(async (entry) => {
        const skillPath = path.join(skillsDir, entry.name, "SKILL.md");
        try {
          const content = await readFile(skillPath, "utf8");
          const match = content.match(/^name:\s*([^\n]+)$/m);
          return match ? match[1].trim() : entry.name;
        } catch {
          return entry.name;
        }
      }));
    return [...new Set(names)].sort();
  } catch {
    return [];
  }
}

function discussionSystemPrompt() {
  return [
    "你是 Requirement Agent。",
    "目标是帮助用户逐步澄清需求。",
    "请使用中文 Markdown 输出，支持标题、列表、表格和代码块。",
    "每次只提出一个核心问题；如果需要给选项，请用 Markdown 列表呈现 2 到 4 个选项。",
    "如果需求已经足够明确，请在回复末尾单独一行输出 [DISCUSSION_COMPLETE]，并用不超过 5 条中文 bullet 总结已确认需求。",
    "不要输出 JSON，除非用户明确要求。"
  ].join("\n");
}

function runSystemPrompt(mode, workspace) {
  const modePrompts = {
    prd: [
      "你是 Product Agent。",
      "请生成拆分后的中文产品设计交付包，不要把所有内容合并到 PRD.md。",
      `工作目录是 ${workspace.projectDir}。`,
      `产品设计产物目录是 ${workspace.artifactDir.projectDesignDir}。`
    ].join("\n"),
    ui: [
      "你是 Design Agent。",
      "请基于上一阶段 PRD 和 UI 设计规范生成中文 UI 原型交付包。",
      `工作目录是 ${workspace.projectDir}。`,
      `UI 原型产物目录是 ${workspace.artifactDir.uiPrototypeDir}。`
    ].join("\n"),
    architecture: [
      "你是 Architecture Agent。",
      "请输出中文系统架构、部署架构和技术选型设计，使用 markdown。",
      `工作目录是 ${workspace.projectDir}。`
    ].join("\n"),
    api: [
      "你是 API Design Agent。",
      "请输出中文接口设计，并包含 OpenAPI 3.0 定义要点。",
      `工作目录是 ${workspace.projectDir}。`
    ].join("\n"),
    database: [
      "你是 Database Design Agent。",
      "请输出中文数据库设计，并包含数据字典、DDL 和迁移脚本要点。",
      `工作目录是 ${workspace.projectDir}。`
    ].join("\n"),
    generate: [
      "你是 Frontend Developer Agent。",
      `请在目录 ${workspace.frontendDir} 内生成真实可运行的前端工程代码。`,
      "必须基于原始需求、PRD、UI 设计规范、UI 原型、组件库、交互说明、响应式设计、架构设计和接口设计生成，不要生成通用模板。",
      "优先生成 Vue 3 / Vite 前端，包含 package.json、src、入口文件、页面/组件、基础样式和 README。",
      "可以直接读写该目录下文件。",
      "完成后请用中文总结已生成内容、运行命令、构建命令，并列出关键文件。"
    ].join("\n"),
    backend: [
      "你是 Backend Developer Agent。",
      `请在目录 ${workspace.backendDir} 内生成可运行的后端代码。`,
      "优先生成 Spring Boot 后端，遵循 controller -> service -> workflow/skill 分层。",
      "完成后请总结接口、运行命令、测试命令和关键文件。"
    ].join("\n"),
    "test-cases": [
      "你是 QA Case Agent。",
      `请在目录 ${workspace.testsDir} 内生成测试用例文档和 Playwright 测试代码。`,
      "测试需覆盖核心路径、校验、错误状态、权限/角色和前后端联动。",
      "完成后请总结测试范围和关键文件。"
    ].join("\n"),
    playwright: [
      "你是 Playwright QA Agent。",
      `请在目录 ${workspace.projectDir} 内执行或准备执行 Playwright 测试。`,
      "可以补齐项目内最小 Playwright 配置和脚本，但不要修改平台根目录配置。",
      "完成后请输出通过/失败情况、报告路径和可操作失败原因。"
    ].join("\n"),
    "fix-tests": [
      "你是 QA Agent。",
      `请在目录 ${workspace.projectDir} 内执行测试并修复失败项。`,
      "完成后请总结修复结果、仍存在的问题和测试状态。"
    ].join("\n")
  };

  return modePrompts[mode] || "你是 Claude 助手，请根据用户要求完成任务。";
}

function buildAllowedTools(mode) {
  if (["generate", "backend", "test-cases", "playwright", "fix-tests", "openspec"].includes(mode)) {
    return ["Read", "Write", "Edit", "Bash"];
  }
  return ["Read", "Write", "Edit"];
}

function buildModePrompt(mode, prompt, workspace) {
  if (mode === "prd") {
    return [
      "/prd-generator",
      "",
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      "请整合 PM 产品产物包能力与 PRD 生成能力，但必须按文件拆分输出，不允许把所有内容塞进 PRD.md。",
      `目标输出目录是 ${workspace.artifactDir.projectDesignDir}。`,
      "必须生成以下文件，目录和文件名保持不变：",
      `- ${path.join(workspace.artifactDir.changeLogDir, "版本说明.md")}：版本说明和变更记录`,
      `- ${path.join(workspace.artifactDir.requirementDir, "PRD.md")}：正式产品需求文档主体，只包含目标、角色、范围、功能需求、非功能需求、验收标准、约束和待确认问题`,
      `- ${path.join(workspace.artifactDir.uiDir, "UI-Design-Spec.md")}：UI 设计规范、页面结构、视觉规范、组件状态和响应式要求`,
      `- ${path.join(workspace.artifactDir.flowDir, "业务流程图.puml")}：业务流程 PlantUML`,
      `- ${path.join(workspace.artifactDir.flowDir, "信息架构图.puml")}：信息架构 PlantUML`,
      `- ${path.join(workspace.artifactDir.flowDir, "页面流转图.puml")}：页面流转 PlantUML`,
      `- ${path.join(workspace.artifactDir.appendixDir, "术语表.md")}：术语、用户故事、WWAS、测试场景和样例数据等附录内容`,
      "PRD.md 禁止包含完整 UI 规范、PlantUML 图全文、术语表全集、用户故事全集、测试场景全集或样例数据全集。",
      "最终回复只输出已生成文件清单和简短说明，不要内联完整文件内容。",
      "",
      "需求：",
      prompt
    ].join("\n");
  }

  if (mode === "ui") {
    return [
      "/ui-ux-pro-max",
      "",
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      "UI 阶段必须基于上一阶段生成的 PRD.md、UI-Design-Spec.md、流程图和术语表设计，不要只基于原始需求生成通用页面。",
      `产品设计目录是 ${workspace.artifactDir.projectDesignDir}。`,
      `UI 原型输出目录是 ${workspace.artifactDir.uiPrototypeDir}。`,
      "必须产出以下文件，目录和文件名保持不变：",
      `- ${path.join(workspace.artifactDir.uiPrototypeDesignDir, "index.html")}：交互式 HTML 原型，覆盖核心页面、导航、表单、结果区、状态区和错误/空状态`,
      `- ${path.join(workspace.artifactDir.uiPrototypeDesignDir, "desktop.svg")}：桌面端设计图`,
      `- ${path.join(workspace.artifactDir.uiPrototypeDesignDir, "mobile.svg")}：移动端设计图`,
      `- ${path.join(workspace.artifactDir.uiPrototypeComponentDir, "组件清单.md")}：组件库文档`,
      `- ${path.join(workspace.artifactDir.uiPrototypeComponentDir, "component-library.html")}：组件库 HTML 设计稿`,
      `- ${path.join(workspace.artifactDir.uiPrototypeComponentDir, "component-library.svg")}：组件库图片设计图`,
      `- ${path.join(workspace.artifactDir.uiPrototypeInteractionDir, "交互说明.md")}：交互路径说明`,
      `- ${path.join(workspace.artifactDir.uiPrototypeInteractionDir, "interaction-flow.svg")}：交互流程图片设计图`,
      `- ${path.join(workspace.artifactDir.uiPrototypeResponsiveDir, "响应式断点参考.md")}：移动端适配文档`,
      `- ${path.join(workspace.artifactDir.uiPrototypeResponsiveDir, "responsive-preview.html")}：移动端/响应式 HTML 设计稿`,
      `- ${path.join(workspace.artifactDir.uiPrototypeResponsiveDir, "mobile-preview.svg")}：移动端适配图片设计图`,
      "最终回复只输出已生成文件清单和简短说明，不要内联完整文件内容。",
      "",
      "需求与产品设计上下文：",
      prompt
    ].join("\n");
  }

  if (mode === "generate") {
    return [
      "/project-scaffold-skill",
      "",
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      "请生成真实前端工程代码，不要只输出说明文档。",
      "必须先读取上下文中列出的本地产物路径，并以 PRD、UI 规范、UI 原型、组件库、交互说明、响应式断点、架构设计、接口设计、数据库设计和已生成后端代码为实现依据。",
      "禁止只按原始需求生成通用模板；禁止实现 ai-software-factory 平台本身。",
      `前端代码必须写入 ${workspace.frontendDir}。`,
      `请读取并遵循产品设计目录：${workspace.artifactDir.projectDesignDir}。`,
      `请读取并遵循 UI 原型目录：${workspace.artifactDir.uiPrototypeDir}。`,
      `必须读取并遵循后端工程目录：${workspace.backendDir}。`,
      "重点参考 PRD.md、UI-Design-Spec.md、index.html、组件清单.md、交互说明.md、响应式断点参考.md、架构设计、接口设计、数据库设计、后端 controller、DTO、entity、README 和 application 配置。",
      "优先生成 Vue 3 / Vite 工程，必须包含 package.json、src/main.js 或 src/main.ts、页面/组件、样式、API client、README.md。",
      "必须基于后端真实接口实现请求，核心业务操作必须调用后端 API。完全禁止 mock、demoData、本地假数据、伪造成功响应或静态假交互；如果后端接口不可用，应显示真实错误状态。",
      "界面必须体现需求中的业务对象、核心流程、表单、状态和结果展示，不要生成默认欢迎页或通用 Dashboard。",
      "最终回复只输出已生成文件清单、运行命令、构建命令和关键说明。",
      "",
      "上下文：",
      prompt
    ].join("\n");
  }

  if (mode === "backend") {
    return [
      "/springboot-api-skill",
      "",
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      "请基于需求、PRD、UI 原型、架构设计、接口设计和数据库设计生成真实后端代码。",
      "必须先读取上下文中列出的本地产物路径，并以 PRD、架构设计、接口设计、数据库设计、UI 交互和业务流程为实现依据。",
      "禁止只按原始需求生成通用 CRUD；禁止实现 ai-software-factory 平台本身。",
      `后端代码必须写入 ${workspace.backendDir}。`,
      "必须生成可运行 Spring Boot 项目，遵循 controller -> service -> workflow/adapter -> repository 分层。",
      "必须使用 H2 + Flyway 作为默认本地开发配置，启动后自动建表并初始化演示数据；必须额外提供 application-prod.yml 生产 profile，使用 PostgreSQL 或 MySQL 连接配置占位，并在 README 中说明如何切换生产数据库。",
      "必须包含 README.md、pom.xml、应用入口、controller、service、repository、entity、DTO、application.yml、application-prod.yml、db/migration/V1__*.sql、db/migration/V2__*.sql 和最小测试。",
      "不要覆盖前端代码；如需联调，只能补充必要的接口说明或配置。",
      "最终回复只输出已生成文件清单、运行命令、测试命令和关键接口。",
      "",
      "上下文：",
      prompt
    ].join("\n");
  }

  if (mode === "test-cases") {
    return [
      "/playwright-test-skill",
      "",
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      "请基于需求产物、前端代码、后端代码、接口设计和数据库设计生成真实测试产物。",
      `测试用例文档目录是 ${workspace.artifactDir.testCasesDir}。`,
      `Playwright 测试目录是 ${path.join(workspace.artifactDir.testDir, "e2e")}。`,
      "必须生成 Markdown 测试用例，且生成至少一个可运行的 Playwright spec。",
      "测试覆盖 happy path、表单校验、错误状态、权限/角色、核心业务流程和前后端联动。",
      "最终回复只输出测试范围、文件清单和执行方式。",
      "",
      "上下文：",
      prompt
    ].join("\n");
  }

  if (mode === "playwright") {
    return [
      "/playwright-test-skill",
      "",
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      `请在生成工程目录 ${workspace.projectDir} 内执行或准备执行 Playwright 测试；测试用例和测试报告产物保留在 ${workspace.artifactDir.testDir}。`,
      "如果缺少项目内 Playwright 配置或 npm scripts，可以在生成工程目录内补齐最小配置。",
      "不要修改平台根目录配置。",
      `必须将执行摘要写入 ${path.join(workspace.artifactDir.testReportsDir, "test-report.md")}。`,
      `如生成 HTML 报告，请保留在 ${path.join(workspace.artifactDir.testReportsDir, "playwright-report")}。`,
      `如生成测试结果，请保留在 ${path.join(workspace.artifactDir.testReportsDir, "test-results")}。`,
      "最终回复必须包含通过/失败数量、报告路径和失败项处理建议。",
      "",
      "上下文：",
      prompt
    ].join("\n");
  }

  if (mode === "architecture") {
    return [
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      "你正在为目标业务应用生成架构设计产物，不是在分析 ai-software-factory 平台或当前仓库本身。",
      "必须读取上下文中列出的 PRD、UI 规范、流程图、术语表、UI 原型、组件清单、交互说明和响应式断点参考。",
      "禁止把 apps/web、apps/server、Claude Runner、AI 软件工厂编排链路写入目标应用架构，除非业务需求本身明确要求。",
      `架构设计输出目录是 ${workspace.artifactDir.architectureDir}。`,
      "必须覆盖：目标业务系统架构、模块职责、数据流、部署架构、技术选型、关键风险与约束。",
      "请使用结构化中文 Markdown 输出。",
      "",
      "目标应用上下文：",
      prompt
    ].join("\n");
  }

  if (mode === "api") {
    return [
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      "你正在为目标业务应用生成接口设计产物，不是在分析 ai-software-factory 平台或当前仓库本身。",
      "必须读取上下文中列出的 PRD、UI 原型和架构设计产物。",
      "必须覆盖：接口清单、资源模型、请求/响应字段、错误码、分页过滤约定、OpenAPI 3.0 定义要点。",
      "请使用结构化中文 Markdown 输出，并包含可转换为 openapi.yaml 的接口信息。",
      "",
      "目标应用上下文：",
      prompt
    ].join("\n");
  }

  if (mode === "database") {
    return [
      "本次为 AI 软件工厂自动化主链路，请不要等待用户确认，也不要反问。",
      "你正在为目标业务应用生成数据库设计产物，不是在分析 ai-software-factory 平台或当前仓库本身。",
      "必须读取上下文中列出的 PRD、接口设计和架构设计产物。",
      "必须覆盖：数据字典、核心表、字段类型、主键外键、索引建议、DDL、迁移脚本。",
      "请使用结构化中文 Markdown 输出，并包含可转换为 SQL 文件的表结构信息。",
      "",
      "目标应用上下文：",
      prompt
    ].join("\n");
  }

  return prompt;
}

function isMissingConversationError(error) {
  const message = error?.message || "";
  return message.includes("No conversation found with session ID") || message.includes("No conversation found");
}

function changeIdCandidates(context) {
  const candidates = [];
  if (context?.changeId) {
    candidates.push(context.changeId);
  }
  if (context?.stageKey) {
    candidates.push(`revise-${context.stageKey}`);
    if (context?.workflowRunId) {
      candidates.push(`revise-${context.stageKey}-${String(context.workflowRunId).slice(0, 8)}`);
    }
  }
  return Array.from(new Set(candidates.filter(Boolean)));
}

function changeRoot(changeId) {
  return path.join(repoRoot(), "openspec", "changes", changeId);
}

function isOpenSpecChangeReady(changeId) {
  const root = changeRoot(changeId);
  return existsSync(path.join(root, "proposal.md")) && existsSync(path.join(root, "tasks.md"));
}

async function isOpenSpecChangeCompleted(changeId) {
  if (existsSync(path.join(repoRoot(), "openspec", "changes", "archive", changeId))) {
    return true;
  }
  const tasks = await readTextIfExists(path.join(changeRoot(changeId), "tasks.md"));
  if (!tasks) {
    return false;
  }
  const taskLines = tasks.split("\n").filter((line) => /^\s*- \[[ xX]\]/.test(line));
  return taskLines.length > 0 && taskLines.every((line) => /^\s*- \[[xX]\]/.test(line));
}

async function reusableChangeId(context) {
  for (const changeId of changeIdCandidates(context)) {
    if (isOpenSpecChangeReady(changeId) && !(await isOpenSpecChangeCompleted(changeId))) {
      return changeId;
    }
  }
  return "";
}

function existingChangeId(context) {
  for (const changeId of changeIdCandidates(context)) {
    if (isOpenSpecChangeReady(changeId)) {
      return changeId;
    }
  }
  return "";
}

function nextAvailableChangeId(context) {
  const base = context?.stageKey ? `revise-${context.stageKey}` : `revise-${Date.now()}`;
  if (!existsSync(changeRoot(base)) && !existsSync(path.join(repoRoot(), "openspec", "changes", "archive", base))) {
    return base;
  }
  for (let index = 2; index < 100; index += 1) {
    const candidate = `${base}-${index}`;
    if (!existsSync(changeRoot(candidate)) && !existsSync(path.join(repoRoot(), "openspec", "changes", "archive", candidate))) {
      return candidate;
    }
  }
  return `${base}-${Date.now()}`;
}


async function runClaudeCommand({ prompt, cwd, systemPrompt, resumeSessionId, allowedTools }) {
  const args = [
    "--print",
    "--output-format",
    "stream-json",
    "--verbose",
    "--permission-mode",
    "acceptEdits",
    "--system-prompt",
    systemPrompt,
    `--allowedTools=${allowedTools.join(",")}`,
    prompt
  ];

  if (resumeSessionId) {
    args.splice(0, 0, "--resume", resumeSessionId);
  }

  let processOutput;
  try {
    processOutput = await runProcess("claude", args, {
      cwd,
      env: {
        ...process.env,
        CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC: "1"
      }
    });
  } catch (error) {
    if (error.stdout) {
      const recoveredPayload = parseClaudeOutput(error.stdout, error.stderr || "");
      if (!recoveredPayload.is_error && (recoveredPayload.result || recoveredPayload.session_id)) {
        return recoveredPayload;
      }
    }
    throw error;
  }

  const payload = parseClaudeOutput(processOutput.stdout, processOutput.stderr);
  if (payload.is_error) {
    throw new Error(payload.result || payload.api_error_status || "Claude CLI returned an error");
  }

  return payload;
}

function runProcess(command, args, options = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd: options.cwd,
      env: options.env || process.env,
      stdio: ["pipe", "pipe", "pipe"]
    });
    let stdout = "";
    let stderr = "";

    child.stdout.on("data", (chunk) => {
      stdout += chunk;
    });
    child.stderr.on("data", (chunk) => {
      stderr += chunk;
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) {
        resolve({ stdout, stderr });
      } else {
        const detail = [stderr.trim(), stdout.trim().split("\n").slice(-3).join("\n").trim()].filter(Boolean).join("\n");
        const error = new Error(detail || `Command failed with exit code ${code}`);
        error.exitCode = code;
        error.stdout = stdout;
        error.stderr = stderr;
        reject(error);
      }
    });

    if (options.input) {
      child.stdin.end(options.input);
    } else {
      child.stdin.end();
    }
  });
}

function parseClaudeOutput(stdout, stderr) {
  const trimmed = stdout.trim();
  if (trimmed) {
    try {
      return JSON.parse(trimmed);
    } catch {
      const result = parseStreamJsonOutput(trimmed);
      if (result) {
        return result;
      }
      const start = trimmed.lastIndexOf("\n{");
      if (start >= 0) {
        try {
          return JSON.parse(trimmed.slice(start + 1));
        } catch {
          // Fall through to the clearer error below.
        }
      }
    }
  }

  throw new Error(stderr?.trim() || `Claude CLI did not return JSON output (stdout length: ${stdout.length})`);
}

function parseStreamJsonOutput(output) {
  let sessionId = null;
  let result = "";
  let isError = false;
  let parsedAny = false;

  for (const line of output.split("\n")) {
    if (!line.trim()) {
      continue;
    }
    let event;
    try {
      event = JSON.parse(line);
    } catch {
      continue;
    }
    parsedAny = true;
    if (event.session_id) {
      sessionId = event.session_id;
    }
    if (event.type === "assistant" && Array.isArray(event.message?.content)) {
      for (const part of event.message.content) {
        if (part.type === "text") {
          result += part.text;
        }
      }
    }
    if (event.type === "content_block_delta" && event.delta?.type === "text_delta") {
      result += event.delta.text || "";
    }
    if (event.type === "result") {
      sessionId = event.session_id || sessionId;
      isError = Boolean(event.is_error);
      if (event.result) {
        result = event.result;
      }
    }
  }
  if (!parsedAny || (!sessionId && !result)) {
    return null;
  }
  return {
    is_error: isError,
    result,
    session_id: sessionId
  };
}

async function resolveProductPackContent(productPackFile, content) {
  const existing = await readExistingProductPack(productPackFile);
  if (existing) {
    return existing;
  }

  const linkedPath = content.match(/`([^`]*pm-product-pack\.md)`/)?.[1] || content.match(/([^\s`]+pm-product-pack\.md)/)?.[1];
  if (linkedPath) {
    const candidates = path.isAbsolute(linkedPath)
      ? [linkedPath]
      : [path.resolve(repoRoot(), linkedPath), path.resolve(process.cwd(), linkedPath)];

    for (const candidate of candidates) {
      const linked = await readExistingProductPack(candidate);
      if (linked) {
        return linked;
      }
    }
  }

  return content;
}

async function readExistingProductPack(filePath) {
  try {
    const content = await readFile(filePath, "utf8");
    return content.includes("产品需求文档 PRD") && content.length > 200 ? content : null;
  } catch {
    return null;
  }
}

async function readTextIfExists(filePath) {
  try {
    return await readFile(filePath, "utf8");
  } catch {
    return null;
  }
}

async function writeFileIfMissing(filePath, content) {
  const existing = await readTextIfExists(filePath);
  if (existing && existing.trim().length > 0) {
    return;
  }
  await writeFile(filePath, content);
}

function buildGeneratedFilesSummary(title, files) {
  return [
    `# ${title}`,
    "",
    ...files.map((file) => `- ${file}`)
  ].join("\n");
}

function extractPrdMarkdown(content) {
  const trimmed = content.trim();
  if (trimmed.startsWith("# PM 产品产物包") || trimmed.startsWith("## 1. 产品需求文档 PRD")) {
    return trimmed;
  }

  const section = extractSection(content, [/产品需求文档\s*PRD/i, /PRD/i], "产品需求文档");
  return `# PRD\n\n${section.replace(/^##\s*\d+[.、]?\s*产品需求文档\s*PRD\s*$/im, "## 产品需求文档")}`;
}

function extractSection(content, headingPatterns, fallbackTitle) {
  const headings = [...content.matchAll(/^##\s*\d*[.、]?\s*(.+?)\s*$/gm)];
  for (let index = 0; index < headings.length; index++) {
    const heading = headings[index];
    const title = heading[1] || "";
    if (!headingPatterns.some((pattern) => pattern.test(title))) {
      continue;
    }
    const start = heading.index;
    const end = headings[index + 1]?.index ?? content.length;
    return content.slice(start, end).trim();
  }

  return [`## ${fallbackTitle}`, "", content.trim()].join("\n");
}

function buildUiDesignSpec(content) {
  const section = extractSection(content, [/UI\s*设计规范/i, /界面设计/i, /视觉规范/i], "UI 设计规范");
  if (section.length > 40) {
    return `# UI 设计规范\n\n${section}`;
  }
  return [
    "# UI 设计规范",
    "",
    "## 设计目标",
    "基于需求阶段确认内容，后续 UI 原型阶段需要进一步细化页面结构、组件状态和响应式适配。",
    "",
    "## 页面与组件建议",
    "- 覆盖核心业务入口、主流程页面、结果展示页面和异常状态。",
    "- 组件需包含默认、加载、禁用、错误和空状态。",
    "",
    "## 视觉与交互原则",
    "- 优先保证信息层级清晰、主操作突出、状态反馈明确。"
  ].join("\n");
}

function buildGlossary(content) {
  const section = extractSection(content, [/术语表/i, /附录/i], "术语表");
  if (section.length > 30) {
    return `# 术语表\n\n${section}`;
  }
  return [
    "# 术语表",
    "",
    "| 术语 | 说明 |",
    "| --- | --- |",
    "| 用户 | 使用该系统完成核心业务流程的人员 |",
    "| 需求产物 | 需求阶段输出的 PRD、设计规范、流程图和附录文档 |",
    "| 验收标准 | 判断功能是否满足需求的可验证条件 |"
  ].join("\n");
}

function buildPlantUmlDiagram(title, kind, content) {
  const summary = content.replace(/[#*_`>|]/g, " ").replace(/\s+/g, " ").trim().slice(0, 80) || "需求阶段确认内容";
  if (kind === "business") {
    return [
      "@startuml",
      `title ${title}`,
      "start",
      ":用户提交需求;",
      ":系统生成需求阶段产物;",
      ":用户确认产物;",
      "stop",
      `note right: ${summary}`,
      "@enduml",
      ""
    ].join("\n");
  }
  if (kind === "page-flow") {
    return [
      "@startuml",
      `title ${title}`,
      "rectangle \"需求输入\" as RequirementInput",
      "rectangle \"需求讨论\" as Discussion",
      "rectangle \"产物展示\" as ArtifactView",
      "rectangle \"后续设计\" as NextDesign",
      "RequirementInput --> Discussion : 澄清需求",
      "Discussion --> ArtifactView : 生成产品设计产物",
      "ArtifactView --> NextDesign : 进入 UI / 架构设计",
      `note right of ArtifactView: ${summary}`,
      "@enduml",
      ""
    ].join("\n");
  }
  return [
    "@startuml",
    `title ${title}`,
    "package 产品设计 {",
    "  [需求输入] --> [需求讨论]",
    "  [需求讨论] --> [需求产物]",
    "  [需求产物] --> [后续设计阶段]",
    "}",
    `note right of [需求产物]: ${summary}`,
    "@enduml",
    ""
  ].join("\n");
}

function extractPrototypeMarkdown(content) {
  const patterns = [
    /^##\s*\d+[.、]?\s*原型(?:说明|设计|图)?\s*$/m,
    /^##\s*\d+[.、]?\s*HTML\s*原型\s*$/im,
    /^##\s*\d+[.、]?\s*交互原型\s*$/m
  ];

  for (const pattern of patterns) {
    const start = content.search(pattern);
    if (start >= 0) {
      const rest = content.slice(start);
      const nextSection = rest.slice(1).search(/^##\s*\d+[.、]?\s*/m);
      return nextSection > 0 ? rest.slice(0, nextSection + 1).trim() : rest.trim();
    }
  }

  return [
    "# 原型说明",
    "",
    "以下内容根据需求阶段产品产物包自动整理，可作为 HTML 原型和后续 UI 设计输入。",
    "",
    content
  ].join("\n");
}

function buildPrototypeHtml(prototypeContent, fullContent) {
  const displayContent = prototypeContent || fullContent;
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>需求阶段 HTML 原型</title>
  <style>
    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f5f7fb; color: #172033; }
    main { max-width: 1120px; margin: 0 auto; padding: 48px 24px; }
    .hero { margin-bottom: 24px; padding: 32px; border-radius: 24px; color: white; background: linear-gradient(135deg, #2563eb, #7c3aed); box-shadow: 0 24px 80px rgba(37, 99, 235, 0.24); }
    .grid { display: grid; grid-template-columns: 260px 1fr; gap: 20px; }
    .panel { background: white; border: 1px solid #e5e7ef; border-radius: 20px; padding: 24px; box-shadow: 0 20px 60px rgba(20, 31, 55, 0.08); }
    .nav { display: grid; gap: 12px; }
    .nav span { padding: 12px 14px; border-radius: 14px; background: #eef2ff; color: #3730a3; font-weight: 700; }
    pre { white-space: pre-wrap; line-height: 1.7; background: #111827; color: #e5e7eb; border-radius: 16px; padding: 20px; overflow: auto; }
  </style>
</head>
<body>
  <main>
    <section class="hero">
      <h1>需求阶段 HTML 原型</h1>
      <p>该原型用于表达需求阶段确认的页面结构、关键路径和交互状态。</p>
    </section>
    <section class="grid">
      <aside class="panel nav">
        <span>入口页面</span>
        <span>核心流程</span>
        <span>结果展示</span>
        <span>异常状态</span>
      </aside>
      <article class="panel">
        <h2>原型说明</h2>
        <pre>${escapeHtml(displayContent)}</pre>
      </article>
    </section>
  </main>
</body>
</html>
`;
}

function contentSummary(content, fallback) {
  return content.replace(/[#*_`>|]/g, " ").replace(/\s+/g, " ").trim().slice(0, 260) || fallback;
}

function buildArchitectureDoc(content) {
  return [
    "# 系统架构设计",
    "",
    "## 架构概览",
    contentSummary(content, "基于需求阶段产物生成系统架构设计。"),
    "",
    "## 模块划分",
    "- 前端应用：负责需求输入、过程展示、产物浏览与用户交互。",
    "- 后端服务：负责工作流编排、任务状态、产物索引和运行时适配。",
    "- Runner 服务：负责调用 Claude Code 并将阶段产物落盘。",
    "- 产物目录：负责归档产品设计、UI 原型、架构、数据库和测试文档。",
    "",
    "## 数据流",
    "用户需求 → 需求讨论 → 产物生成 → 架构/API/数据库设计 → 代码开发 → 验证交付。"
  ].join("\n");
}

function buildSystemArchitecturePuml(content) {
  const summary = contentSummary(content, "AI 软件工厂主链路系统架构。");
  return [
    "@startuml",
    "title 系统架构图",
    "actor 用户 as User",
    "rectangle \"Web 前端\\nVue 3\" as Web",
    "rectangle \"后端服务\\nSpring Boot\" as Server",
    "rectangle \"Claude Runner\\nNode.js\" as Runner",
    "rectangle \"Claude Code CLI\" as ClaudeCode",
    "database \"产物目录\\ndocs/generated\" as Artifacts",
    "User --> Web : 输入需求 / 查看产物",
    "Web --> Server : REST API",
    "Server --> Runner : 阶段任务",
    "Runner --> ClaudeCode : claude -p / skills",
    "ClaudeCode --> Runner : 阶段输出",
    "Runner --> Artifacts : 写入文档 / 图 / 代码",
    "Server --> Artifacts : 索引与访问",
    "Web --> Server : 打开产物 URL",
    `note bottom of Artifacts: ${summary}`,
    "@enduml",
    ""
  ].join("\n");
}

function buildSystemArchitectureSvg(content) {
  const summary = escapeXml(contentSummary(content, "AI 软件工厂主链路系统架构。"));
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="720" viewBox="0 0 1280 720">
  <defs><filter id="shadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="16" stdDeviation="18" flood-color="#1e293b" flood-opacity="0.14"/></filter></defs>
  <rect width="1280" height="720" fill="#f8fafc"/>
  <text x="72" y="84" font-family="Arial, PingFang SC, sans-serif" font-size="36" font-weight="700" fill="#172033">系统架构图</text>
  <g font-family="Arial, PingFang SC, sans-serif" font-size="18" fill="#172033">
    <rect x="80" y="164" width="180" height="96" rx="24" fill="#ffffff" stroke="#bfdbfe" filter="url(#shadow)"/><text x="150" y="220" font-weight="700">用户</text>
    <rect x="340" y="164" width="220" height="96" rx="24" fill="#ffffff" stroke="#bfdbfe" filter="url(#shadow)"/><text x="398" y="206" font-weight="700">Web 前端</text><text x="418" y="236" fill="#64748b">Vue 3</text>
    <rect x="640" y="164" width="240" height="96" rx="24" fill="#ffffff" stroke="#bfdbfe" filter="url(#shadow)"/><text x="704" y="206" font-weight="700">后端服务</text><text x="704" y="236" fill="#64748b">Spring Boot</text>
    <rect x="960" y="164" width="240" height="96" rx="24" fill="#ffffff" stroke="#bfdbfe" filter="url(#shadow)"/><text x="1018" y="206" font-weight="700">Claude Runner</text><text x="1042" y="236" fill="#64748b">Node.js</text>
    <rect x="960" y="372" width="240" height="96" rx="24" fill="#eff6ff" stroke="#93c5fd" filter="url(#shadow)"/><text x="1012" y="430" font-weight="700">Claude Code CLI</text>
    <rect x="640" y="372" width="240" height="112" rx="24" fill="#ecfdf5" stroke="#86efac" filter="url(#shadow)"/><text x="704" y="420" font-weight="700">产物目录</text><text x="692" y="452" fill="#64748b">docs/generated</text>
  </g>
  <g stroke="#2563eb" stroke-width="5" stroke-linecap="round" fill="none">
    <path d="M270 212 H330"/><path d="M320 200 L338 212 L320 224"/>
    <path d="M570 212 H630"/><path d="M620 200 L638 212 L620 224"/>
    <path d="M890 212 H950"/><path d="M940 200 L958 212 L940 224"/>
    <path d="M1080 272 V360"/><path d="M1068 350 L1080 368 L1092 350"/>
    <path d="M950 420 H890"/><path d="M900 408 L882 420 L900 432"/>
    <path d="M760 272 V360"/><path d="M748 350 L760 368 L772 350"/>
  </g>
  <text x="72" y="616" font-family="Arial, PingFang SC, sans-serif" font-size="18" fill="#475569">${summary}</text>
</svg>`;
}

function buildDeploymentDoc(content) {
  return [
    "# 部署架构",
    "",
    "## 部署单元",
    "- Web 前端：静态资源服务或开发服务器。",
    "- Spring Boot 后端：提供 API、状态查询和产物访问。",
    "- Claude Runner：独立 Node.js 进程，负责执行 Claude Code。",
    "- 文件存储：本地 docs/generated/workspace 目录，后续可替换为对象存储。",
    "",
    "## 运行依赖",
    "- Java 21",
    "- Node.js / Yarn",
    "- Claude Code CLI",
    "",
    "## 依据",
    contentSummary(content, "部署架构需支持本地开发和后续服务化部署。")
  ].join("\n");
}

function buildTechSelectionDoc(content) {
  return [
    "# 技术选型",
    "",
    "| 层级 | 技术 | 说明 |",
    "| --- | --- | --- |",
    "| 前端 | Vue 3 / Vite | 快速构建交互式工作台 |",
    "| 后端 | Spring Boot | 提供清晰 controller-service-workflow 分层 |",
    "| Runner | Node.js | 封装 Claude Code CLI 调用和文件落盘 |",
    "| 测试 | JUnit / Playwright | 覆盖服务逻辑和端到端流程 |",
    "| 产物 | Markdown / PlantUML / OpenAPI / SQL | 便于审阅、版本管理和后续生成 |",
    "",
    "## 依据",
    contentSummary(content, "技术选型优先复用当前仓库技术栈。")
  ].join("\n");
}

function buildApiList(content) {
  return [
    "# 接口清单",
    "",
    "| 方法 | 路径 | 用途 | 请求 | 响应 |",
    "| --- | --- | --- | --- | --- |",
    "| POST | /api/requirements | 提交需求并创建任务 | 需求文本 | 任务 ID 和初始状态 |",
    "| GET | /api/workflow/status | 查询工作流状态 | 任务 ID | 阶段、进度、Agent 状态 |",
    "| GET | /api/artifacts | 访问生成产物 | 文件路径 | 文件内容或下载流 |",
    "| POST | /api/tests/playwright | 触发端到端测试 | 测试用例 ID | 测试运行结果 |",
    "",
    "## 设计依据",
    contentSummary(content, "接口设计基于需求和前端交互路径生成。")
  ].join("\n");
}

function buildOpenApiYaml(content) {
  const description = contentSummary(content, "AI 软件工厂接口定义").replaceAll('"', "'");
  return [
    "openapi: 3.0.3",
    "info:",
    "  title: AI Software Factory API",
    "  version: 1.0.0",
    `  description: \"${description}\"`,
    "paths:",
    "  /api/workflow/status:",
    "    get:",
    "      summary: 查询工作流状态",
    "      responses:",
    "        '200':",
    "          description: 当前工作流状态",
    "  /api/artifacts:",
    "    get:",
    "      summary: 访问生成产物",
    "      parameters:",
    "        - in: query",
    "          name: path",
    "          schema:",
    "            type: string",
    "          required: true",
    "      responses:",
    "        '200':",
    "          description: 产物内容",
    ""
  ].join("\n");
}

function buildDataDictionary(content) {
  return [
    "# 数据字典",
    "",
    "| 表名 | 字段 | 类型 | 说明 |",
    "| --- | --- | --- | --- |",
    "| workflow_runs | id | varchar(64) | 工作流任务 ID |",
    "| workflow_runs | requirement | text | 用户原始需求 |",
    "| workflow_runs | status | varchar(32) | 当前状态 |",
    "| artifacts | id | varchar(64) | 产物 ID |",
    "| artifacts | workflow_id | varchar(64) | 所属工作流 |",
    "| artifacts | path | text | 产物路径 |",
    "",
    "## 设计依据",
    contentSummary(content, "数据字典基于需求、接口和产物流转设计。")
  ].join("\n");
}

function buildSchemaSql(content) {
  return [
    "-- Schema generated for AI Software Factory design stage",
    "CREATE TABLE IF NOT EXISTS workflow_runs (",
    "  id VARCHAR(64) PRIMARY KEY,",
    "  requirement TEXT NOT NULL,",
    "  status VARCHAR(32) NOT NULL,",
    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,",
    "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
    ");",
    "",
    "CREATE TABLE IF NOT EXISTS artifacts (",
    "  id VARCHAR(64) PRIMARY KEY,",
    "  workflow_id VARCHAR(64) NOT NULL,",
    "  stage VARCHAR(64) NOT NULL,",
    "  name VARCHAR(255) NOT NULL,",
    "  path TEXT NOT NULL,",
    "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
    ");",
    "",
    "CREATE INDEX IF NOT EXISTS idx_artifacts_workflow_id ON artifacts(workflow_id);",
    ""
  ].join("\n");
}

function buildMigrationSql(content) {
  return [
    "-- Migration generated for AI Software Factory design stage",
    "-- Apply after reviewing target database dialect.",
    "ALTER TABLE workflow_runs ADD COLUMN IF NOT EXISTS current_stage VARCHAR(128);",
    "ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS artifact_type VARCHAR(64);",
    ""
  ].join("\n");
}

function buildComponentCatalog(content) {
  const summary = content.replace(/[#*_`>|]/g, " ").replace(/\s+/g, " ").trim().slice(0, 180) || "基于 UI 设计规范整理组件库。";
  return [
    "# 组件清单",
    "",
    "## 组件总览",
    `本组件库基于 UI 阶段输出整理：${summary}`,
    "",
    "| 组件 | 用途 | 关键状态 | 可访问性要求 |",
    "| --- | --- | --- | --- |",
    "| 顶部导航 | 承载产品名称、主操作和全局状态 | 默认、悬停、聚焦 | 导航区域使用语义化标签 |",
    "| 需求输入表单 | 收集用户需求或筛选条件 | 默认、输入中、错误、禁用、提交中 | 字段必须有可见标签和错误提示 |",
    "| 阶段进度条 | 展示工作流阶段和执行状态 | 待开始、进行中、成功、失败 | 状态不能仅依赖颜色表达 |",
    "| 结果卡片 | 展示文档、原型和代码产物入口 | 空状态、可打开、不可用 | 按钮需有明确文本标签 |",
    "| 日志列表 | 展示执行过程和系统反馈 | 空状态、滚动、错误 | 内容更新区域使用 polite 提示 |",
    "",
    "## 设计约束",
    "- 触控目标不小于 44px。",
    "- 正文对比度不低于 4.5:1。",
    "- 组件状态需覆盖加载、错误、空数据和禁用状态。"
  ].join("\n");
}

function buildInteractionPrototype(content) {
  const summary = content.replace(/[#*_`>|]/g, " ").replace(/\s+/g, " ").trim().slice(0, 180) || "基于 UI 设计规范整理交互路径。";
  return [
    "# 交互说明",
    "",
    "## 关键路径",
    "1. 用户进入页面并输入需求。",
    "2. 系统进入需求讨论，逐步澄清关键信息。",
    "3. 用户确认讨论结果后，系统生成需求产物和 UI 原型。",
    "4. 用户在结果面板查看、打开或下载产物。",
    "",
    "## 状态反馈",
    "- 提交后按钮进入加载态并避免重复提交。",
    "- 每个工作流阶段需要展示进行中、成功、失败和等待状态。",
    "- 失败状态需要提供清晰原因和重试入口。",
    "",
    "## 异常与空状态",
    "- 无日志时展示空状态提示。",
    "- 无产物时展示等待生成。",
    "- 网络失败时展示可恢复错误说明。",
    "",
    "## 输入依据",
    summary
  ].join("\n");
}

function buildResponsiveReference(content) {
  const summary = content.replace(/[#*_`>|]/g, " ").replace(/\s+/g, " ").trim().slice(0, 180) || "基于 UI 设计规范整理响应式适配。";
  return [
    "# 响应式断点参考",
    "",
    "| 断点 | 设备类型 | 布局策略 |",
    "| --- | --- | --- |",
    "| 375px | 小屏手机 | 单列布局，核心操作优先，隐藏次要装饰 |",
    "| 768px | 平板/大屏手机横向 | 双列或上下分区，保留结果摘要 |",
    "| 1024px | 小桌面/平板横向 | 主工作区与侧栏并列展示 |",
    "| 1440px | 桌面端 | 增加内容宽度和分组密度，保持阅读行长 |",
    "",
    "## 适配原则",
    "- 移动端优先，避免横向滚动。",
    "- 固定区域不得遮挡内容，保留安全区和滚动内边距。",
    "- 长文本优先换行而非截断。",
    "- 所有交互目标在触屏设备上不小于 44px。",
    "",
    "## 输入依据",
    summary
  ].join("\n");
}

function buildUiDesignSvg(content) {
  const summary = escapeXml(content.replace(/[#*_`>\-]/g, " ").replace(/\s+/g, " ").trim()).slice(0, 260);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="860" viewBox="0 0 1280 860">
  <defs>
    <linearGradient id="bg" x1="0" x2="1" y1="0" y2="1"><stop offset="0" stop-color="#eef2ff"/><stop offset="1" stop-color="#f8fafc"/></linearGradient>
    <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%"><feDropShadow dx="0" dy="18" stdDeviation="24" flood-color="#1e293b" flood-opacity="0.16"/></filter>
  </defs>
  <rect width="1280" height="860" fill="url(#bg)"/>
  <rect x="72" y="64" width="1136" height="732" rx="32" fill="#ffffff" filter="url(#shadow)"/>
  <rect x="72" y="64" width="1136" height="92" rx="32" fill="#2563eb"/>
  <text x="120" y="122" font-family="Arial, PingFang SC, sans-serif" font-size="30" font-weight="700" fill="#ffffff">UI 设计图</text>
  <rect x="120" y="204" width="220" height="520" rx="24" fill="#f1f5f9"/>
  <rect x="376" y="204" width="760" height="156" rx="24" fill="#dbeafe"/>
  <rect x="376" y="396" width="360" height="328" rx="24" fill="#f8fafc" stroke="#dbe3ef"/>
  <rect x="776" y="396" width="360" height="328" rx="24" fill="#f8fafc" stroke="#dbe3ef"/>
  <circle cx="172" cy="268" r="24" fill="#2563eb"/><rect x="214" y="250" width="88" height="18" rx="9" fill="#94a3b8"/>
  <circle cx="172" cy="340" r="24" fill="#7c3aed"/><rect x="214" y="322" width="96" height="18" rx="9" fill="#94a3b8"/>
  <circle cx="172" cy="412" r="24" fill="#0f766e"/><rect x="214" y="394" width="78" height="18" rx="9" fill="#94a3b8"/>
  <text x="416" y="268" font-family="Arial, PingFang SC, sans-serif" font-size="26" font-weight="700" fill="#172033">主页面视觉层级</text>
  <text x="416" y="318" font-family="Arial, PingFang SC, sans-serif" font-size="18" fill="#475569">${summary || "基于 UI 规范生成的页面布局、组件层级和状态表达。"}</text>
  <text x="416" y="456" font-family="Arial, PingFang SC, sans-serif" font-size="22" font-weight="700" fill="#172033">核心内容区</text>
  <text x="816" y="456" font-family="Arial, PingFang SC, sans-serif" font-size="22" font-weight="700" fill="#172033">状态与操作区</text>
  <rect x="416" y="500" width="280" height="18" rx="9" fill="#cbd5e1"/><rect x="416" y="544" width="220" height="18" rx="9" fill="#e2e8f0"/><rect x="416" y="588" width="250" height="18" rx="9" fill="#e2e8f0"/>
  <rect x="816" y="500" width="160" height="48" rx="16" fill="#2563eb"/><rect x="996" y="500" width="88" height="48" rx="16" fill="#e2e8f0"/><rect x="816" y="584" width="268" height="18" rx="9" fill="#cbd5e1"/>
</svg>`;
}

function buildMobileDesignSvg(content) {
  const summary = escapeXml(contentSummary(content, "基于 PRD 和 UI 规范生成移动端页面。"));
  return `<svg xmlns="http://www.w3.org/2000/svg" width="430" height="932" viewBox="0 0 430 932">
  <rect width="430" height="932" fill="#eef2ff"/>
  <rect x="38" y="32" width="354" height="868" rx="42" fill="#111827"/>
  <rect x="54" y="72" width="322" height="780" rx="30" fill="#ffffff"/>
  <rect x="78" y="104" width="274" height="72" rx="22" fill="#2563eb"/>
  <text x="100" y="149" font-family="Arial, PingFang SC, sans-serif" font-size="20" font-weight="700" fill="#ffffff">移动端主流程</text>
  <rect x="78" y="204" width="274" height="108" rx="22" fill="#dbeafe"/>
  <rect x="102" y="232" width="176" height="14" rx="7" fill="#2563eb"/>
  <rect x="102" y="264" width="220" height="12" rx="6" fill="#93c5fd"/>
  <rect x="78" y="336" width="274" height="156" rx="22" fill="#f8fafc" stroke="#dbe3ef"/>
  <rect x="102" y="364" width="214" height="16" rx="8" fill="#64748b"/>
  <rect x="102" y="404" width="226" height="44" rx="14" fill="#eff6ff"/>
  <rect x="78" y="520" width="274" height="64" rx="20" fill="#2563eb"/>
  <text x="154" y="560" font-family="Arial, PingFang SC, sans-serif" font-size="18" font-weight="700" fill="#ffffff">提交 / 查看结果</text>
  <text x="78" y="642" font-family="Arial, PingFang SC, sans-serif" font-size="14" fill="#475569">${summary}</text>
</svg>`;
}

function buildComponentLibraryHtml(content) {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>组件库设计稿</title>
  <style>
    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #f8fafc; color: #172033; }
    main { max-width: 1100px; margin: 0 auto; padding: 40px 24px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 18px; }
    .card { border: 1px solid #e2e8f0; border-radius: 22px; padding: 22px; background: #fff; box-shadow: 0 16px 50px rgba(15, 23, 42, .08); }
    .button { display: inline-flex; min-height: 44px; align-items: center; padding: 0 18px; border-radius: 14px; background: #2563eb; color: #fff; font-weight: 700; }
    .input { height: 44px; border-radius: 14px; border: 1px solid #cbd5e1; padding: 0 14px; display: flex; align-items: center; color: #64748b; }
    pre { white-space: pre-wrap; background: #0f172a; color: #e2e8f0; border-radius: 18px; padding: 18px; overflow: auto; }
  </style>
</head>
<body>
  <main>
    <h1>组件库设计稿</h1>
    <p>组件状态基于 PRD、UI 设计规范和核心用户流程整理。</p>
    <section class="grid">
      <article class="card"><h2>主按钮</h2><span class="button">主要操作</span><p>默认、悬停、聚焦、加载、禁用。</p></article>
      <article class="card"><h2>输入表单</h2><div class="input">请输入关键业务信息</div><p>必填、错误、帮助文本、提交中。</p></article>
      <article class="card"><h2>结果卡片</h2><p>展示产物摘要、状态和打开入口。</p></article>
      <article class="card"><h2>阶段进度</h2><p>待开始、进行中、成功、失败。</p></article>
    </section>
    <h2>设计依据</h2>
    <pre>${escapeHtml(contentSummary(content, "基于产品设计产物生成组件库。"))}</pre>
  </main>
</body>
</html>`;
}

function buildComponentLibrarySvg(content) {
  const summary = escapeXml(contentSummary(content, "组件库覆盖主要组件和状态。"));
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="760" viewBox="0 0 1200 760">
  <rect width="1200" height="760" fill="#f8fafc"/>
  <text x="72" y="88" font-family="Arial, PingFang SC, sans-serif" font-size="34" font-weight="700" fill="#172033">组件库设计图</text>
  <rect x="72" y="132" width="304" height="220" rx="28" fill="#ffffff" stroke="#dbe3ef"/>
  <text x="112" y="190" font-family="Arial, PingFang SC, sans-serif" font-size="24" font-weight="700" fill="#172033">按钮</text>
  <rect x="112" y="226" width="148" height="52" rx="16" fill="#2563eb"/>
  <text x="146" y="259" font-family="Arial, PingFang SC, sans-serif" font-size="16" font-weight="700" fill="#fff">主要操作</text>
  <rect x="448" y="132" width="304" height="220" rx="28" fill="#ffffff" stroke="#dbe3ef"/>
  <text x="488" y="190" font-family="Arial, PingFang SC, sans-serif" font-size="24" font-weight="700" fill="#172033">表单</text>
  <rect x="488" y="226" width="216" height="52" rx="16" fill="#f8fafc" stroke="#cbd5e1"/>
  <rect x="824" y="132" width="304" height="220" rx="28" fill="#ffffff" stroke="#dbe3ef"/>
  <text x="864" y="190" font-family="Arial, PingFang SC, sans-serif" font-size="24" font-weight="700" fill="#172033">结果卡片</text>
  <rect x="864" y="226" width="216" height="20" rx="10" fill="#cbd5e1"/>
  <rect x="864" y="270" width="156" height="20" rx="10" fill="#e2e8f0"/>
  <text x="72" y="454" font-family="Arial, PingFang SC, sans-serif" font-size="20" fill="#475569">${summary}</text>
</svg>`;
}

function buildInteractionFlowSvg(content) {
  const summary = escapeXml(contentSummary(content, "交互流程覆盖输入、处理、结果和异常状态。"));
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="520" viewBox="0 0 1200 520">
  <rect width="1200" height="520" fill="#eef2ff"/>
  <text x="72" y="86" font-family="Arial, PingFang SC, sans-serif" font-size="34" font-weight="700" fill="#172033">交互流程图</text>
  <g font-family="Arial, PingFang SC, sans-serif" font-size="18" font-weight="700" fill="#172033">
    <rect x="88" y="174" width="190" height="96" rx="24" fill="#ffffff" stroke="#bfdbfe"/><text x="132" y="230">进入/输入</text>
    <path d="M292 222 H406" stroke="#2563eb" stroke-width="6" stroke-linecap="round"/>
    <rect x="420" y="174" width="190" height="96" rx="24" fill="#ffffff" stroke="#bfdbfe"/><text x="462" y="230">校验/提交</text>
    <path d="M624 222 H738" stroke="#2563eb" stroke-width="6" stroke-linecap="round"/>
    <rect x="752" y="174" width="190" height="96" rx="24" fill="#ffffff" stroke="#bfdbfe"/><text x="790" y="230">处理/反馈</text>
    <path d="M956 222 H1070" stroke="#2563eb" stroke-width="6" stroke-linecap="round"/>
    <rect x="982" y="330" width="190" height="96" rx="24" fill="#ffffff" stroke="#fecaca"/><text x="1030" y="386">异常恢复</text>
  </g>
  <text x="72" y="456" font-family="Arial, PingFang SC, sans-serif" font-size="18" fill="#475569">${summary}</text>
</svg>`;
}

function buildResponsivePreviewHtml(content) {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>响应式适配预览</title>
  <style>
    body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; background: #eef2ff; color: #172033; }
    main { padding: 28px; display: grid; gap: 20px; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); }
    .device { background: #fff; border: 1px solid #dbe3ef; border-radius: 28px; padding: 22px; box-shadow: 0 16px 50px rgba(15, 23, 42, .08); }
    .screen { min-height: 280px; border-radius: 22px; background: linear-gradient(180deg, #dbeafe, #f8fafc); padding: 18px; display: grid; gap: 14px; align-content: start; }
    .bar, .card { border-radius: 14px; background: #fff; min-height: 44px; }
    .cta { border-radius: 14px; background: #2563eb; min-height: 48px; }
  </style>
</head>
<body>
  <main>
    <section class="device"><h1>375px 手机</h1><div class="screen"><div class="bar"></div><div class="card"></div><div class="card"></div><div class="cta"></div></div></section>
    <section class="device"><h1>768px 平板</h1><div class="screen"><div class="bar"></div><div class="card"></div><div class="card"></div><div class="cta"></div></div></section>
    <section class="device"><h1>1440px 桌面</h1><div class="screen"><div class="bar"></div><div class="card"></div><div class="card"></div><div class="cta"></div></div></section>
    <section class="device"><h1>设计依据</h1><p>${escapeHtml(contentSummary(content, "基于 PRD 和 UI 规范生成响应式适配预览。"))}</p></section>
  </main>
</body>
</html>`;
}

function escapeXml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

async function writeModeArtifacts(mode, workspace, content) {
  if (mode === "prd") {
    const prdMarkdownFile = path.join(workspace.artifactDir.requirementDir, "PRD.md");
    const versionFile = path.join(workspace.artifactDir.changeLogDir, "版本说明.md");
    const uiSpecFile = path.join(workspace.artifactDir.uiDir, "UI-Design-Spec.md");
    const businessFlowFile = path.join(workspace.artifactDir.flowDir, "业务流程图.puml");
    const infoArchitectureFile = path.join(workspace.artifactDir.flowDir, "信息架构图.puml");
    const pageFlowFile = path.join(workspace.artifactDir.flowDir, "页面流转图.puml");
    const glossaryFile = path.join(workspace.artifactDir.appendixDir, "术语表.md");
    const summaryFile = path.join(workspace.projectDir, "product-design-artifacts.md");

    await Promise.all([
      writeFileIfMissing(prdMarkdownFile, extractPrdMarkdown(content)),
      writeFileIfMissing(versionFile, "# 版本说明\n\n- V1.0：首次生成产品设计产物。\n"),
      writeFileIfMissing(uiSpecFile, buildUiDesignSpec(content)),
      writeFileIfMissing(businessFlowFile, buildPlantUmlDiagram("业务流程图", "business", content)),
      writeFileIfMissing(infoArchitectureFile, buildPlantUmlDiagram("信息架构图", "information", content)),
      writeFileIfMissing(pageFlowFile, buildPlantUmlDiagram("页面流转图", "page-flow", content)),
      writeFileIfMissing(glossaryFile, buildGlossary(content))
    ]);

    const artifacts = [
      prdMarkdownFile,
      versionFile,
      uiSpecFile,
      businessFlowFile,
      infoArchitectureFile,
      pageFlowFile,
      glossaryFile
    ];
    await writeFile(summaryFile, buildGeneratedFilesSummary("产品设计产物已生成", artifacts));
    return artifacts;
  }

  if (mode === "ui") {
    const guidelinesFile = path.join(workspace.artifactDir.uiDir, "UI-Design-Spec.md");
    const prototypeHtmlFile = path.join(workspace.artifactDir.uiPrototypeDesignDir, "index.html");
    const desktopDesignFile = path.join(workspace.artifactDir.uiPrototypeDesignDir, "desktop.svg");
    const mobileDesignFile = path.join(workspace.artifactDir.uiPrototypeDesignDir, "mobile.svg");
    const componentCatalogFile = path.join(workspace.artifactDir.uiPrototypeComponentDir, "组件清单.md");
    const componentHtmlFile = path.join(workspace.artifactDir.uiPrototypeComponentDir, "component-library.html");
    const componentImageFile = path.join(workspace.artifactDir.uiPrototypeComponentDir, "component-library.svg");
    const interactionFile = path.join(workspace.artifactDir.uiPrototypeInteractionDir, "交互说明.md");
    const interactionImageFile = path.join(workspace.artifactDir.uiPrototypeInteractionDir, "interaction-flow.svg");
    const responsiveFile = path.join(workspace.artifactDir.uiPrototypeResponsiveDir, "响应式断点参考.md");
    const responsiveHtmlFile = path.join(workspace.artifactDir.uiPrototypeResponsiveDir, "responsive-preview.html");
    const responsiveImageFile = path.join(workspace.artifactDir.uiPrototypeResponsiveDir, "mobile-preview.svg");

    await Promise.all([
      writeFileIfMissing(guidelinesFile, buildUiDesignSpec(content)),
      writeFileIfMissing(prototypeHtmlFile, buildPrototypeHtml(extractPrototypeMarkdown(content), content)),
      writeFileIfMissing(desktopDesignFile, buildUiDesignSvg(content)),
      writeFileIfMissing(mobileDesignFile, buildMobileDesignSvg(content)),
      writeFileIfMissing(componentCatalogFile, buildComponentCatalog(content)),
      writeFileIfMissing(componentHtmlFile, buildComponentLibraryHtml(content)),
      writeFileIfMissing(componentImageFile, buildComponentLibrarySvg(content)),
      writeFileIfMissing(interactionFile, buildInteractionPrototype(content)),
      writeFileIfMissing(interactionImageFile, buildInteractionFlowSvg(content)),
      writeFileIfMissing(responsiveFile, buildResponsiveReference(content)),
      writeFileIfMissing(responsiveHtmlFile, buildResponsivePreviewHtml(content)),
      writeFileIfMissing(responsiveImageFile, buildMobileDesignSvg(content))
    ]);

    return [
      guidelinesFile,
      prototypeHtmlFile,
      desktopDesignFile,
      mobileDesignFile,
      componentCatalogFile,
      componentHtmlFile,
      componentImageFile,
      interactionFile,
      interactionImageFile,
      responsiveFile,
      responsiveHtmlFile,
      responsiveImageFile
    ];
  }

  if (mode === "backend") {
    const backendDir = workspace.backendDir;
    const readmeFile = path.join(backendDir, "README.md");
    const summaryFile = path.join(backendDir, "backend-summary.md");
    await ensureDirectory(backendDir);
    await writeFileIfMissing(readmeFile, buildGeneratedFilesSummary("后端工程", [backendDir, summaryFile]));
    await writeFile(summaryFile, content);
    return [backendDir, readmeFile, summaryFile];
  }

  if (mode === "test-cases") {
    const testRootDir = workspace.artifactDir.testDir;
    const testCasesDir = workspace.artifactDir.testCasesDir;
    const e2eDir = path.join(testRootDir, "e2e");
    const summaryFile = path.join(testCasesDir, "test-case-summary.md");
    await Promise.all([ensureDirectory(testCasesDir), ensureDirectory(e2eDir)]);
    await writeFile(summaryFile, content);
    return [testRootDir, testCasesDir, e2eDir, summaryFile];
  }

  if (mode === "playwright") {
    const reportFile = path.join(workspace.artifactDir.testReportsDir, "test-report.md");
    const playwrightReportDir = path.join(workspace.artifactDir.testReportsDir, "playwright-report");
    const testResultsDir = path.join(workspace.artifactDir.testReportsDir, "test-results");
    const e2eDir = path.join(workspace.artifactDir.testDir, "e2e");
    await Promise.all([ensureDirectory(playwrightReportDir), ensureDirectory(testResultsDir), ensureDirectory(e2eDir)]);
    await writeFile(reportFile, content);
    return [reportFile, playwrightReportDir, testResultsDir, e2eDir];
  }

  if (mode === "architecture") {
    const systemFile = path.join(workspace.artifactDir.architectureSystemDir, "系统架构设计.md");
    const systemPumlFile = path.join(workspace.artifactDir.architectureSystemDir, "系统架构图.puml");
    const systemSvgFile = path.join(workspace.artifactDir.architectureSystemDir, "系统架构图.svg");
    const deploymentFile = path.join(workspace.artifactDir.architectureDeployDir, "部署架构.md");
    const techSelectionFile = path.join(workspace.artifactDir.architectureTechDir, "技术选型.md");

    await Promise.all([
      writeFile(systemFile, buildArchitectureDoc(content)),
      writeFile(systemPumlFile, buildSystemArchitecturePuml(content)),
      writeFile(systemSvgFile, buildSystemArchitectureSvg(content)),
      writeFile(deploymentFile, buildDeploymentDoc(content)),
      writeFile(techSelectionFile, buildTechSelectionDoc(content))
    ]);

    return [systemFile, systemPumlFile, systemSvgFile, deploymentFile, techSelectionFile];
  }

  if (mode === "api") {
    const apiListFile = path.join(workspace.artifactDir.architectureApiDir, "接口清单.md");
    const openApiFile = path.join(workspace.artifactDir.architectureApiDefinitionsDir, "openapi.yaml");

    await Promise.all([
      writeFile(apiListFile, buildApiList(content)),
      writeFile(openApiFile, buildOpenApiYaml(content))
    ]);

    return [apiListFile, openApiFile];
  }

  if (mode === "database") {
    const dictionaryFile = path.join(workspace.artifactDir.databaseDictionaryDir, "数据字典.md");
    const schemaFile = path.join(workspace.artifactDir.databaseDDLDir, "schema.sql");
    const migrationFile = path.join(workspace.artifactDir.databaseMigrationDir, "migration.sql");

    await Promise.all([
      writeFile(dictionaryFile, buildDataDictionary(content)),
      writeFile(schemaFile, buildSchemaSql(content)),
      writeFile(migrationFile, buildMigrationSql(content))
    ]);

    return [dictionaryFile, schemaFile, migrationFile];
  }

  if (mode === "fix-tests") {
    const file = path.join(workspace.projectDir, "test-report.md");
    await writeFile(file, content);
    return [file];
  }

  return [workspace.projectDir];
}

function escapeHtml(value) {
  return value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function discussionComplete(content) {
  return content.includes("[DISCUSSION_COMPLETE]") || content.includes("[讨论完成]");
}

export async function checkEnvironment() {
  const cliInstalled = await checkCliInstalled();
  const loggedIn = cliInstalled ? await detectLoginStatus() : false;

  return {
    runnerOnline: true,
    cliInstalled,
    loggedIn,
    workingDirectory: process.cwd(),
    workspaceRoot: defaultWorkspaceRoot(),
    docsRoot: defaultDocsRoot(),
    availableSkills: await detectAvailableSkills(),
    permissions: {
      read: true,
      write: true,
      bash: cliInstalled,
      cliAuth: loggedIn
    }
  };
}

export async function startSession({ taskId, requirement = "", workspaceRoot }) {
  const workspace = await ensureWorkspace(taskId, requirement, workspaceRoot);
  const sessionId = createSessionId(taskId);
  const session = {
    taskId,
    sessionId,
    claudeSessionId: null,
    workspaceDir: workspace.taskRoot,
    projectDir: workspace.projectDir,
    docsProjectDir: workspace.projectDesignDir,
    projectName: workspace.projectName,
    contextDir: workspace.contextDir,
    requirement,
    history: requirement ? [{ role: "user", content: `我的需求是：${requirement}` }] : [],
    createdAt: new Date().toISOString(),
    provider: "claude-cli"
  };

  sessions.set(taskId, session);
  await persistJson(workspace.contextDir, "session.json", session);

  return {
    taskId,
    sessionId,
    workspaceDir: workspace.taskRoot,
    projectDir: workspace.projectDir,
    docsProjectDir: workspace.projectDesignDir,
    projectName: workspace.projectName,
    history: session.history
  };
}

export async function sendMessage({ taskId, sessionId, prompt, workspaceRoot }) {
  const session = sessions.get(taskId) || await loadPersistedSession(taskId, workspaceRoot);
  if (!session || session.sessionId !== sessionId) {
    throw new Error(`Session not found for taskId=${taskId}`);
  }

  const command = {
    prompt,
    cwd: session.workspaceDir,
    systemPrompt: discussionSystemPrompt(),
    resumeSessionId: session.claudeSessionId,
    allowedTools: buildAllowedTools("discussion")
  };
  let payload;
  try {
    payload = await runClaudeCommand(command);
  } catch (error) {
    if (!command.resumeSessionId || !isMissingConversationError(error)) {
      throw error;
    }
    session.claudeSessionId = null;
    payload = await runClaudeCommand({ ...command, resumeSessionId: null });
  }

  session.claudeSessionId = payload.session_id || session.claudeSessionId;
  session.history.push({ role: "user", content: prompt });
  session.history.push({ role: "ai", content: payload.result || "" });
  await persistJson(session.contextDir, "session.json", session);

  return {
    taskId,
    sessionId,
    status: "success",
    content: payload.result || "",
    history: session.history,
    isComplete: discussionComplete(payload.result || ""),
    logs: [
      {
        time: new Date().toISOString(),
        level: "info",
        message: "Processed discussion message with Claude CLI"
      }
    ]
  };
}

export async function runTask({ taskId, mode, prompt, workspaceRoot, sessionId, agentId }) {
  const existingSession = sessions.get(taskId) || await loadPersistedSession(taskId, workspaceRoot);
  const requirement = existingSession?.requirement || prompt;
  const workspace = await ensureWorkspace(taskId, requirement, workspaceRoot);
  const resolvedSessionId = sessionId || existingSession?.sessionId || createSessionId(taskId);
  const session = existingSession || {
    taskId,
    sessionId: resolvedSessionId,
    claudeSessionId: null,
    workspaceDir: workspace.taskRoot,
    projectDir: workspace.projectDir,
    docsProjectDir: workspace.projectDesignDir,
    projectName: workspace.projectName,
    contextDir: workspace.contextDir,
    requirement,
    history: [],
    createdAt: new Date().toISOString(),
    provider: "claude-cli"
  };

  sessions.set(taskId, session);

  const modeCwd = {
    generate: workspace.frontendDir,
    backend: workspace.backendDir,
    "test-cases": workspace.projectDir,
    playwright: workspace.projectDir,
    "fix-tests": workspace.projectDir
  };
  const modeSystemPrompt = runSystemPrompt(mode, workspace);
  const agentPrompt = await loadAgentPrompt(agentId);
  const command = {
    prompt: buildModePrompt(mode, prompt, workspace),
    cwd: modeCwd[mode] || repoRoot(),
    systemPrompt: mergeSystemPrompt(agentPrompt.prompt, modeSystemPrompt),
    resumeSessionId: session.claudeSessionId,
    allowedTools: buildAllowedTools(mode)
  };
  let payload;
  try {
    payload = await runClaudeCommand(command);
  } catch (error) {
    if (!command.resumeSessionId || !isMissingConversationError(error)) {
      throw error;
    }
    session.claudeSessionId = null;
    payload = await runClaudeCommand({ ...command, resumeSessionId: null });
  }

  session.claudeSessionId = payload.session_id || session.claudeSessionId;
  session.history.push({ role: "user", content: `[${mode}] ${prompt}` });
  session.history.push({ role: "ai", content: payload.result || "" });
  await persistJson(session.contextDir, "session.json", session);

  const artifacts = await writeModeArtifacts(mode, workspace, payload.result || "");

  return {
    taskId,
    sessionId: resolvedSessionId,
    status: "success",
    content: payload.result || "",
    artifacts,
    logs: [
      {
        time: new Date().toISOString(),
        level: "info",
        message: `Executed ${mode || "custom"} task with Claude CLI${agentPrompt.agentId ? ` using ${agentPrompt.agentId}` : ""}`
      }
    ],
    workspaceDir: workspace.taskRoot,
    projectDir: workspace.projectDir,
    docsProjectDir: workspace.projectDesignDir,
    projectName: workspace.projectName
  };
}

function buildOpenSpecPrompt(action, context) {
  const artifactPaths = Array.isArray(context.artifactPaths) ? context.artifactPaths : [];
  const lines = [
    `/${action === "explore" ? "opsx:explore" : action === "propose" ? "opsx:propose" : action === "apply" ? "opsx:apply" : "opsx:archive"}${context.changeId && action !== "explore" && action !== "propose" ? ` ${context.changeId}` : ""}`,
    "",
    "这是 AI 软件工厂阶段产物修订操作，请基于当前阶段上下文执行。",
    `workflowRunId: ${context.workflowRunId}`,
    `stageKey: ${context.stageKey}`,
    `stageTitle: ${context.stageTitle}`,
    `projectName: ${context.projectName || "未命名项目"}`,
    `changeId: ${context.changeId || "未创建"}`,
    `允许修改范围: ${context.allowedPaths || "当前阶段相关产物"}`,
    "",
    "当前阶段产物路径:",
    ...(artifactPaths.length ? artifactPaths.map((item) => `- ${item}`) : ["- 暂无产物路径"]),
    "",
    "用户反馈:",
    context.userFeedback || "用户尚未填写具体反馈。"
  ];

  if (action === "explore") {
    lines.push("", "请只探索当前阶段产物如何修改，不要直接实现代码。输出阶段修订建议、风险和下一步计划。 ");
  }
  if (action === "propose") {
    lines.push("", `请为当前阶段修订创建 OpenSpec change。建议 changeId：${context.changeId || `revise-${context.stageKey}`}`);
  }
  if (action === "apply") {
    lines.push("", "请执行该 OpenSpec change，但必须限制在允许修改范围内。完成后总结修改文件。 ");
  }
  if (action === "archive") {
    lines.push("", "请归档该 OpenSpec change。归档后不要推进主工作流阶段。 ");
  }

  return lines.join("\n");
}

async function extractChangeId(action, context, content) {
  const reusable = action === "propose" ? "" : await reusableChangeId(context);
  if (reusable) {
    return reusable;
  }
  if (context.changeId && action !== "propose") {
    return context.changeId;
  }
  const text = content || "";
  const matched = text.match(/(?:change(?:Id)?|Change ID|变更|change)[：:\s`]*([a-z0-9][a-z0-9-]{3,})/i);
  if (matched?.[1]) {
    return matched[1];
  }
  if (action === "propose") {
    return `revise-${context.stageKey}-${String(context.workflowRunId || Date.now()).slice(0, 8)}`;
  }
  return context.changeId || "";
}

export async function runOpenSpecAction({ taskId, action, context, workspaceRoot }) {
  const workspace = await ensureWorkspace(taskId, context?.projectName || "", workspaceRoot);
  const resolvedContext = { ...(context || {}) };
  const reusable = action === "propose" ? "" : await reusableChangeId(resolvedContext);
  if (action === "propose") {
    resolvedContext.changeId = nextAvailableChangeId(resolvedContext);
  } else if (reusable && action !== "explore") {
    resolvedContext.changeId = reusable;
  }
  const prompt = buildOpenSpecPrompt(action, resolvedContext);
  let payload;
  try {
    payload = await runClaudeCommand({
      prompt,
      cwd: repoRoot(),
      systemPrompt: "你是 OpenSpec 阶段修订执行代理。严格遵循用户指定的 /opsx 操作和允许修改范围，输出简洁中文结果。",
      resumeSessionId: null,
      allowedTools: buildAllowedTools("openspec")
    });
  } catch (error) {
    const changeId = existingChangeId(resolvedContext);
    if (changeId) {
      return {
        taskId,
        action,
        changeId,
        status: "success",
        content: `检测到 OpenSpec change 已创建：\`${changeId}\`\n\n位置：\`openspec/changes/${changeId}/\`\n\nClaude CLI 退出状态异常，但 proposal.md 与 tasks.md 已落盘，可继续执行计划。`,
        updatedAt: new Date().toISOString(),
        workspaceDir: workspace.taskRoot
      };
    }
    throw error;
  }
  const content = payload.result || "";

  return {
    taskId,
    action,
    changeId: await extractChangeId(action, resolvedContext, content),
    status: "success",
    content,
    updatedAt: new Date().toISOString(),
    workspaceDir: workspace.taskRoot
  };
}

export async function closeSession({ taskId, sessionId, workspaceRoot }) {
  const session = sessions.get(taskId) || await loadPersistedSession(taskId, workspaceRoot);
  if (!session || session.sessionId !== sessionId) {
    throw new Error(`Session not found for taskId=${taskId}`);
  }

  sessions.delete(taskId);
  return {
    taskId,
    sessionId,
    status: "closed"
  };
}
