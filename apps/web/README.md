# AI Software Factory - Frontend

基于 Vue 3 + Vite 的前端应用，提供工作流编排的可视化界面。

## 项目架构

```
apps/web/
├── src/
│   ├── main.js              # 应用入口
│   ├── App.vue              # 根组件
│   ├── components/          # 可复用组件
│   │   └── WorkflowBoard.vue    # 工作流画板组件
│   ├── views/               # 页面视图
│   │   └── HomeView.vue         # 首页
│   ├── stores/              # Pinia 状态管理
│   │   └── workflow.js          # 工作流状态
│   ├── router/              # Vue Router 路由配置
│   │   └── index.js
│   ├── api/                 # API 客户端
│   │   ├── client.js           # Axios 实例封装
│   │   ├── health.js           # 健康检查 API
│   │   └── workflow.js          # 工作流 API
│   └── workflow/            # 工作流定义
│       └── defaultPipeline.js  # 默认流水线配置
├── index.html               # HTML 入口
├── vite.config.js           # Vite 配置
└── package.json
```

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Pinia** - Vue 状态管理
- **Vue Router** - 官方路由管理器
- **Axios** - HTTP 客户端

## 依赖安装

```bash
# 进入前端目录
cd apps/web

# 安装依赖
yarn install
# 或
npm install
```

## 启动方式

### 开发模式

```bash
# 在 apps/web 目录下
yarn dev
# 或
npm run dev
```

开发服务器将在 `http://localhost:5173` 启动。

API 请求会自动代理到后端 `http://127.0.0.1:8080`。

### 构建生产版本

```bash
yarn build
# 或
npm run build
```

构建产物输出到 `dist/` 目录。

### 预览生产构建

```bash
yarn preview
# 或
npm run preview
```

## 环境要求

- Node.js >= 18
- yarn 或 npm