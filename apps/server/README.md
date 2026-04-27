# AI Software Factory - Backend

基于 Spring Boot 的后端 API 服务，提供工作流编排、技能执行和 LLM 集成能力。

## 项目架构

```
apps/server/
├── src/
│   ├── main/
│   │   ├── java/com/aifactory/
│   │   │   ├── AiFactoryApplication.java  # 应用入口
│   │   │   ├── controller/                 # REST 控制器
│   │   │   │   ├── HealthController.java
│   │   │   │   └── WorkflowController.java
│   │   │   ├── service/                    # 业务服务层
│   │   │   │   ├── HealthService.java
│   │   │   │   └── WorkflowService.java
│   │   │   ├── workflow/                   # 工作流编排
│   │   │   │   ├── WorkflowStage.java
│   │   │   │   └── DesignArtifactFactory.java
│   │   │   ├── skill/                      # 技能执行层
│   │   │   │   ├── Skill.java                 # 技能接口
│   │   │   │   ├── SkillRegistry.java         # 技能注册表
│   │   │   │   ├── SkillExecution.java        # 技能执行器
│   │   │   │   ├── ShellSkill.java             # Shell 命令技能
│   │   │   │   ├── PlaywrightSkill.java       # Playwright 测试技能
│   │   │   │   ├── FileWriteSkill.java        # 文件写入技能
│   │   │   │   ├── TemplateRenderSkill.java   # 模板渲染技能
│   │   │   │   ├── PrdSkillAdapter.java        # PRD 技能适配器
│   │   │   │   ├── UiGenerateSkillAdapter.java # UI 生成技能适配器
│   │   │   │   ├── BrainstormingSkillAdapter.java    # 需求讨论技能适配器 (superpowers)
│   │   │   │   ├── WritingPlansSkillAdapter.java      # 实现计划技能适配器 (superpowers)
│   │   │   │   └── VerificationBeforeCompletionSkillAdapter.java # 验证完成技能适配器 (superpowers)
│   │   │   ├── llm/                         # LLM 集成层
│   │   │   │   ├── LlmClient.java             # LLM 客户端接口
│   │   │   │   ├── TemplateLlmClient.java     # 模板 LLM 客户端
│   │   │   │   ├── ModelRouter.java           # 模型路由
│   │   │   │   ├── LlmRequest.java
│   │   │   │   └── LlmResponse.java
│   │   │   ├── agent/                       # Agent 定义
│   │   │   │   ├── AgentDefinition.java
│   │   │   │   └── AgentStatus.java
│   │   │   ├── dto/                         # 数据传输对象
│   │   │   │   ├── WorkflowStatus.java
│   │   │   │   ├── StartWorkflowRequest.java
│   │   │   │   ├── StepStatus.java
│   │   │   │   └── ...
│   │   │   ├── config/                      # 配置类
│   │   │   │   └── WebConfig.java
│   │   │   └── common/                      # 公共组件
│   │   │       └── ApiResponse.java
│   │   └── resources/
│   │       ├── prompts/                     # 提示词模板
│   │       └── application.properties
│   └── test/                                # 测试代码
│       └── java/com/aifactory/
├── pom.xml
└── .mvn/                                    # Maven Wrapper
```

## 技术栈

- **Spring Boot 3.3.5** - 应用框架
- **Java 21** - 运行时环境
- **Maven** - 构建工具

## 依赖安装

```bash
# 进入后端目录
cd apps/server

# 使用 Maven Wrapper 安装依赖
./mvnw dependency:resolve
# 或使用系统 Maven
mvn dependency:resolve
```

## 启动方式

### 开发模式

```bash
# 在 apps/server 目录下
# 设置环境变量
export GLM_API_KEY="your_api_key_from_open.bigmodel.cn"
./mvnw spring-boot:run
# 或使用系统 Maven
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动。

### 构建 JAR 包

```bash
./mvnw clean package
# 或
mvn clean package
```

构建产物在 `target/` 目录。

### 运行 JAR 包

```bash
java -jar target/server-0.1.0.jar
```

## API 端点

### 工作流 API
- `GET /api/health` - 健康检查
- `POST /api/workflow/start` - 启动工作流
- `GET /api/workflow/{id}/status` - 查询工作流状态

### 需求讨论 API
- `POST /api/discussion/start` - 开始需求讨论，返回第一个问题
- `POST /api/discussion/chat` - 发送用户回复，返回下一个问题
- `POST /api/discussion/confirm` - 确认讨论结束，启动工作流
- `GET /api/discussion/{id}/history` - 获取讨论历史

### 日志与结果 API
- `GET /api/logs` - 获取实时日志
- `DELETE /api/logs` - 清空日志
- `GET /api/result` - 获取执行结果

## 工作流执行流程

当用户输入需求后，WorkflowService 按以下顺序调用技能：

```
1. brainstorming-skill → 需求讨论与设计（生成设计文档）
2. writing-plans-skill → 制定实现计划（生成实现步骤）
3. prd-skill → 生成 PRD（产品需求文档）
4. ui-generate-skill → 生成 UI 规范（页面清单与组件建议）
5. verification-skill → 验证完成（运行验证步骤）
```

每个阶段的输出文档保存在 `docs/superpowers/` 目录：
- `specs/YYYY-MM-DD-<topic>-design.md` — 设计文档
- `plans/YYYY-MM-DD-<feature-name>.md` — 实现计划

## 环境要求

- JDK 21+
- Maven 3.6+（或使用内置 Maven Wrapper）

## LLM 配置

### 使用真实 LLM API（推荐）

设置环境变量启用 LLM：

```bash
# 阿里云 DashScope Anthropic 兼容接口
export LLM_API_KEY="your_dashscope_api_key"
export LLM_API_ENDPOINT="https://coding.dashscope.aliyuncs.com/apps/anthropic"
export LLM_API_TYPE="anthropic"
export LLM_MODEL="claude-3-5-sonnet-20241022"

# 或使用智谱 GLM
export LLM_API_KEY="your_glm_api_key"
export LLM_API_ENDPOINT="https://open.bigmodel.cn/api/paas/v4/chat/completions"
export LLM_API_TYPE="openai"
export LLM_MODEL="glm-4-flash"

# 或使用 OpenAI
export LLM_API_KEY="your_openai_api_key"
export LLM_API_ENDPOINT="https://api.openai.com/v1/chat/completions"
export LLM_API_TYPE="openai"
export LLM_MODEL="gpt-4o"
```

获取阿里云 DashScope API Key：
1. 注册 https://dashscope.console.aliyun.com
2. 创建 API Key
3. 在应用管理中找到 Anthropic 兼容接口地址

### 未配置 API Key 时

未配置 `LLM_API_KEY` 时，系统使用模板客户端返回预定义内容，适合测试。