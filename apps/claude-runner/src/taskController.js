import {
  checkEnvironment,
  closeSession,
  runTask,
  sendMessage,
  startSession
} from "./claudeRunner.js";

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    req.on("data", (chunk) => {
      body += chunk;
    });
    req.on("end", () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (error) {
        reject(new Error("Invalid JSON body"));
      }
    });
    req.on("error", reject);
  });
}

function sendJson(res, statusCode, payload) {
  res.writeHead(statusCode, { "Content-Type": "application/json; charset=utf-8" });
  res.end(JSON.stringify(payload));
}

function validateRequired(body, fields) {
  const missing = fields.filter((field) => !body[field]);
  if (missing.length > 0) {
    throw new Error(`Missing required fields: ${missing.join(", ")}`);
  }
}

export async function handleClaudeRequest(req, res) {
  try {
    if (req.method === "GET" && req.url === "/claude/env") {
      return sendJson(res, 200, await checkEnvironment());
    }

    if (req.method === "POST" && req.url === "/claude/session/start") {
      const body = await readJsonBody(req);
      validateRequired(body, ["taskId"]);
      return sendJson(res, 200, await startSession(body));
    }

    if (req.method === "POST" && req.url === "/claude/session/message") {
      const body = await readJsonBody(req);
      validateRequired(body, ["taskId", "sessionId", "prompt"]);
      return sendJson(res, 200, await sendMessage(body));
    }

    if (req.method === "POST" && req.url === "/claude/session/close") {
      const body = await readJsonBody(req);
      validateRequired(body, ["taskId", "sessionId"]);
      return sendJson(res, 200, await closeSession(body));
    }

    if (req.method === "POST" && req.url === "/claude/run") {
      const body = await readJsonBody(req);
      validateRequired(body, ["taskId", "mode", "prompt"]);
      return sendJson(res, 200, await runTask(body));
    }

    return sendJson(res, 404, { error: "Not Found" });
  } catch (error) {
    return sendJson(res, 400, {
      error: error.message || "Unknown error"
    });
  }
}
