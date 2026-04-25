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
│   │   │   │   └── UiGenerateSkillAdapter.java # UI 生成技能适配器
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

- `GET /api/health` - 健康检查
- `POST /api/workflow/start` - 启动工作流
- `GET /api/workflow/{id}/status` - 查询工作流状态

## 环境要求

- JDK 21+
- Maven 3.6+（或使用内置 Maven Wrapper）