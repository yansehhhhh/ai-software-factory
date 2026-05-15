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

## Workspace 与生成产物

任务运行时上下文保留在：

```text
workspace/runtime/{taskId}/
```

运行时目录包含：
- `logs/`
- `context/`
- `metadata.json`

前后端工程代码不写入运行时 workspace，而是按项目英文名写入：

```text
generated/{项目英文名}/
├── frontend/
├── backend/
└── tests/
```

例如 `docs/移动端应用：会议室预约系统` 对应的工程目录为 `generated/HX-Meeting/`。
