import { access, mkdir, readFile, writeFile } from "node:fs/promises";
import { constants } from "node:fs";
import path from "node:path";
import process from "node:process";
import { execFile } from "node:child_process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);
const sessions = new Map();
const DEFAULT_MODEL = process.env.CLAUDE_MODEL || "sonnet";

function defaultWorkspaceRoot() {
  return path.resolve(process.cwd(), "workspace", "runs");
}

async function ensureDirectory(dirPath) {
  await mkdir(dirPath, { recursive: true });
}

async function ensureWorkspace(taskId, workspaceRoot = defaultWorkspaceRoot()) {
  const taskRoot = path.join(workspaceRoot, taskId);
  const projectDir = path.join(taskRoot, "project");
  const logsDir = path.join(taskRoot, "logs");
  const contextDir = path.join(taskRoot, "context");

  await Promise.all([
    ensureDirectory(taskRoot),
    ensureDirectory(projectDir),
    ensureDirectory(logsDir),
    ensureDirectory(contextDir)
  ]);

  const metadataPath = path.join(taskRoot, "metadata.json");
  try {
    await access(metadataPath, constants.F_OK);
  } catch {
    await writeFile(metadataPath, JSON.stringify({ taskId, createdAt: new Date().toISOString() }, null, 2));
  }

  return { taskRoot, projectDir, logsDir, contextDir, metadataPath };
}

function createSessionId(taskId) {
  return `${taskId}-${Date.now()}`;
}

async function persistJson(contextDir, fileName, content) {
  const target = path.join(contextDir, fileName);
  await writeFile(target, JSON.stringify(content, null, 2));
  return target;
}

async function checkCliInstalled() {
  try {
    await execFileAsync("claude", ["--version"]);
    return true;
  } catch {
    return false;
  }
}

async function detectLoginStatus() {
  try {
    const { stdout } = await execFileAsync("claude", ["auth", "status"]);
    const status = JSON.parse(stdout);
    return Boolean(status.loggedIn);
  } catch {
    return false;
  }
}

async function detectAvailableSkills() {
  const claudeMdPath = path.resolve(process.cwd(), "..", "..", "CLAUDE.md");
  try {
    const listing = await readFile(claudeMdPath, "utf8");
    const matches = [...listing.matchAll(/`\/([\w-]+)`/g)].map((match) => match[1]);
    return [...new Set(matches)];
  } catch {
    return [];
  }
}

function discussionSystemPrompt() {
  return [
    "你是 Requirement Agent。",
    "目标是帮助用户逐步澄清需求。",
    "每次只提出一个问题。",
    "如果需求已经足够明确，请只输出 [DISCUSSION_COMPLETE]，然后用不超过 5 条中文 bullet 总结已确认需求。"
  ].join("\n");
}

function runSystemPrompt(mode, workspace) {
  const modePrompts = {
    prd: [
      "你是 Product Agent。",
      "请输出结构化中文 PRD，使用 markdown。",
      `工作目录是 ${workspace.projectDir}。`
    ].join("\n"),
    ui: [
      "你是 Design Agent。",
      "请输出中文 UI 设计规范，使用 markdown。",
      `工作目录是 ${workspace.projectDir}。`
    ].join("\n"),
    generate: [
      "你是 Developer Agent。",
      `请在目录 ${workspace.projectDir} 内生成工程代码。`,
      "可以直接读写该目录下文件。",
      "完成后请用中文总结已生成内容，并列出关键文件。"
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
  if (mode === "generate" || mode === "fix-tests") {
    return ["Read", "Write", "Edit", "Bash"];
  }
  return ["Read", "Write", "Edit"];
}

async function runClaudeCommand({ prompt, cwd, systemPrompt, resumeSessionId, allowedTools }) {
  const args = [
    "-p",
    "--output-format",
    "json",
    "--permission-mode",
    "acceptEdits",
    "--model",
    DEFAULT_MODEL,
    "--system-prompt",
    systemPrompt,
    `--allowedTools=${allowedTools.join(",")}`,
    prompt
  ];

  if (resumeSessionId) {
    args.splice(0, 0, "--resume", resumeSessionId);
  }

  const { stdout, stderr } = await execFileAsync("claude", args, {
    cwd,
    maxBuffer: 10 * 1024 * 1024,
    env: {
      ...process.env,
      CLAUDE_CODE_DISABLE_NONESSENTIAL_TRAFFIC: "1"
    }
  });

  const lines = stdout.split("\n").map((line) => line.trim()).filter(Boolean);
  const jsonLine = [...lines].reverse().find((line) => line.startsWith("{"));
  if (!jsonLine) {
    throw new Error(stderr?.trim() || "Claude CLI did not return JSON output");
  }

  const payload = JSON.parse(jsonLine);
  if (payload.is_error) {
    throw new Error(payload.result || payload.api_error_status || "Claude CLI returned an error");
  }

  return payload;
}

async function writeModeArtifacts(mode, workspace, content) {
  if (mode === "prd") {
    const file = path.join(workspace.projectDir, "prd.md");
    await writeFile(file, content);
    return [file];
  }

  if (mode === "ui") {
    const file = path.join(workspace.projectDir, "ui-guidelines.md");
    await writeFile(file, content);
    return [file];
  }

  if (mode === "fix-tests") {
    const file = path.join(workspace.projectDir, "test-report.md");
    await writeFile(file, content);
    return [file];
  }

  return [workspace.projectDir];
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
  const workspace = await ensureWorkspace(taskId, workspaceRoot);
  const sessionId = createSessionId(taskId);
  const session = {
    taskId,
    sessionId,
    claudeSessionId: null,
    workspaceDir: workspace.taskRoot,
    projectDir: workspace.projectDir,
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
    history: session.history
  };
}

export async function sendMessage({ taskId, sessionId, prompt }) {
  const session = sessions.get(taskId);
  if (!session || session.sessionId !== sessionId) {
    throw new Error(`Session not found for taskId=${taskId}`);
  }

  const payload = await runClaudeCommand({
    prompt,
    cwd: session.workspaceDir,
    systemPrompt: discussionSystemPrompt(),
    resumeSessionId: session.claudeSessionId,
    allowedTools: buildAllowedTools("discussion")
  });

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

export async function runTask({ taskId, mode, prompt, workspaceRoot, sessionId }) {
  const workspace = await ensureWorkspace(taskId, workspaceRoot);
  const resolvedSessionId = sessionId || sessions.get(taskId)?.sessionId || createSessionId(taskId);
  const session = sessions.get(taskId) || {
    taskId,
    sessionId: resolvedSessionId,
    claudeSessionId: null,
    workspaceDir: workspace.taskRoot,
    projectDir: workspace.projectDir,
    contextDir: workspace.contextDir,
    history: [],
    createdAt: new Date().toISOString(),
    provider: "claude-cli"
  };

  sessions.set(taskId, session);

  const payload = await runClaudeCommand({
    prompt,
    cwd: mode === "generate" || mode === "fix-tests" ? workspace.projectDir : workspace.taskRoot,
    systemPrompt: runSystemPrompt(mode, workspace),
    resumeSessionId: session.claudeSessionId,
    allowedTools: buildAllowedTools(mode)
  });

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
        message: `Executed ${mode || "custom"} task with Claude CLI`
      }
    ],
    workspaceDir: workspace.taskRoot,
    projectDir: workspace.projectDir
  };
}

export async function closeSession({ taskId, sessionId }) {
  const session = sessions.get(taskId);
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
