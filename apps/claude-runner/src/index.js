import http from "node:http";
import { handleClaudeRequest } from "./taskController.js";

const port = Number(process.env.PORT || 7001);

const server = http.createServer(async (req, res) => {
  if (req.method === "GET" && req.url === "/health") {
    res.writeHead(200, { "Content-Type": "application/json; charset=utf-8" });
    res.end(JSON.stringify({ status: "UP" }));
    return;
  }

  await handleClaudeRequest(req, res);
});

server.listen(port, () => {
  console.log(`Claude Runner listening on http://localhost:${port}`);
});
