# Claude Runner

Node 服务，负责把 Spring Boot 的任务请求转成 Claude Code 会话执行。

## 启动

```bash
yarn workspace @ai-factory/claude-runner dev
```

默认端口：`7001`

## API

- `GET /health`
- `GET /claude/env`
- `POST /claude/session/start`
- `POST /claude/session/message`
- `POST /claude/session/close`
- `POST /claude/run`

## Workspace

所有任务运行在：

```text
workspace/runs/{taskId}/
```

目录包含：
- `project/`
- `logs/`
- `context/`
- `metadata.json`
