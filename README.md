# 🏋️ 个人健身训练规划 Agent

基于 **DeepSeek Function Call + LangChain4j** 的多角色健身训练规划 Agent。用户用自然语言描述身体情况与健身目标，后端经过「意图解析 → RAG 检索 → 规划 → Critic 评审 → 汇总」的流水线，生成结构化 Markdown 训练方案，并支持**分享链接 + 二维码**，他人扫码即可只读查看。

> 无本地大模型、无向量库。DeepSeek 在线 API 提供推理与 Function Call，健身动作知识库用词法检索实现 RAG。

---

## ✨ 核心特性

- **多角色 Agent 流水线**：IntentExtractor → Planner → Critic → Summarizer，Critic 不通过时自动回传意见重规划（最多 3 次，防死循环）。
- **Function Call 工具**：通过 Java SPI 可插拔，内置 BMI 计算、每日热量推荐两个工具。
- **RAG 词法检索**：本地 40+ 健身动作知识库，按关键词/器械/部位打分，**硬性排除**与伤病禁忌冲突的动作。
- **JSON Schema 强校验**：LLM 输出非法 JSON 时自动注入修正 prompt 重试。
- **轻量状态机**：`待解析 → 规划中 → 评审中 → 已完成/失败`，非法迁移直接报错。
- **MySQL 持久化 + Redis 会话缓存**：任务记录落库，会话上下文缓存减少重复 LLM 调用。
- **分享 + 二维码**：前端 qrcode.js 浏览器本地生成二维码，后端不出图；分享页只读，不暴露用户原始输入。

---

## 🧭 整体架构

```
浏览器 ──POST /api/task/submit──▶ TaskController ──▶ TaskService
                                                     │ 创建记录 + 生成 sessionId
                                                     ├─▶ Redis 缓存会话上下文
                                                     └─▶ MySQL 持久化任务记录
                                                          │
                                        AgentOrchestrator（@Async 后台线程）
                                                          │
            IntentExtractor ──▶ Planner(Function Call + RAG) ──▶ Critic ──▶ Summarizer
            （结构化提取）      （生成一周计划，可调工具）      （评审）   （Markdown）
                                                          │
                                            Critic 不通过且迭代<3 时回传 Planner 重生成
```

### Mermaid 架构图

```mermaid
flowchart TD
    A[用户 / 分享者浏览器] -->|POST /api/task/submit| B[TaskController]
    B --> C[TaskService<br/>创建任务 & 生成 sessionId]
    C --> D[(Redis<br/>会话上下文缓存)]
    C --> E[(MySQL<br/>任务持久化)]
    C --> F[AgentOrchestrator<br/>@Async 后台执行]

    subgraph Pipeline[多角色 Agent 流水线]
        F --> G[TaskStateMachine<br/>PENDING_PARSE → PLANNING → REVIEWING]
        G --> H[IntentExtractor Agent<br/>DeepSeek 结构化提取]
        H --> I[Planner Agent<br/>Function Call + RAG]
        I --> J[Critic Agent<br/>评审校验]
        J -->|不通过 且 迭代<3| I
        J -->|通过| K[Summarizer Agent<br/>输出 Markdown 方案]
    end

    K --> E
    K --> D
    E -->|GET result / 分享链接| A

    I -.->|ToolSpecification| L[SPI 工具<br/>BMI 计算 / 热量推荐]
    I -.->|词法检索| M[ExerciseKnowledgeBase<br/>exercises.json]
    E -->|GET /api/task/{id}/share| N[share.html?sessionId=xxx<br/>只读查看 + qrcode.js 二维码]
```

---

## 🛠 技术栈

| 层 | 技术 |
|---|---|
| 语言 / 框架 | Java 17 · Spring Boot 3.3 · Spring Data JPA · Spring Data Redis |
| 大模型 | DeepSeek 在线 API（`deepseek-chat`，OpenAI 兼容 + Function Call） |
| LLM SDK | LangChain4j 1.2.0（唯一大模型 SDK） |
| 存储 | MySQL 8 · Redis 7 |
| JSON 校验 | networknt json-schema-validator |
| 前端 | 原生 HTML + JS + qrcode.js + marked.js（静态页，打包进 jar） |

---

## 📁 包结构

```
src/main/java/com/fitness/agent/
├── FitnessAgentApplication.java     # 启动类（@EnableAsync）
├── config/                          # LangChain4j / 线程池 / 配置属性
├── controller/TaskController.java   # 提交 / 进度 / 结果 / 分享
├── service/
│   ├── AgentOrchestrator.java       # 流水线编排（@Async）
│   ├── TaskService.java             # 任务 CRUD
│   ├── IntentExtractorAgent.java    # 意图解析
│   ├── PlannerAgent.java            # 规划（Function Call + RAG）
│   ├── CriticAgent.java             # 评审
│   ├── SummarizerAgent.java         # Markdown 汇总
│   ├── ExerciseKnowledgeBase.java   # 知识库加载 + 词法检索
│   └── SessionCacheService.java     # Redis 会话缓存（容错降级）
├── agent/                           # JsonExtractor / JsonSchemaValidator / AgentToolCaller
├── statemachine/                    # TaskStatus / TaskStateMachine
├── tool/                            # AgentTool(SPI) / BMI / 热量推荐
├── model/                           # 实体 + DTO
├── repository/TaskRepository.java
├── exception/                       # BusinessException / GlobalExceptionHandler
└── util/JsonUtil.java

src/main/resources/
├── application.yml                  # 端口 / DB / Redis / DeepSeek / Agent 参数
├── prompts.yml                      # 4 个角色 prompt + 修正模板（改文案不碰代码）
├── schema/*.json                    # Profile / Plan / Critic 的 JSON Schema
├── data/exercises.json              # 健身动作知识库
├── META-INF/services/...AgentTool   # SPI 工具登记
└── static/                          # index.html / share.html / css / js
```

---

## 🚀 快速开始（本地）

### 1. 环境要求

- JDK 17+
- Maven 3.6+
- Docker（用于一键启动 MySQL + Redis；也可用本地已有的 MySQL/Redis）

### 2. 启动 MySQL + Redis

```bash
docker compose up -d
```

> 默认：MySQL `root/root123456`、库 `fitness_agent`；Redis `localhost:6379`。可在 `application.yml` 中修改。

### 3. 配置 DeepSeek API Key

```bash
# Windows (PowerShell)
$env:DEEPSEEK_API_KEY="sk-你的key"

# macOS / Linux
export DEEPSEEK_API_KEY="sk-你的key"
```

或直接修改 `src/main/resources/application.yml` 中的 `deepseek.api-key`（**不要提交到 git**）。

### 4. 启动

```bash
mvn spring-boot:run
```

浏览器打开 <http://localhost:8080>，输入健身需求即可。

---

## 📦 打包上线部署

```bash
# 打包可执行 jar
mvn clean package -DskipTests

# 上传 target/fitness-training-agent-1.0.0.jar 到服务器后启动
java -jar fitness-training-agent-1.0.0.jar
```

生产环境建议：

```bash
# 1. 配置环境变量
export DEEPSEEK_API_KEY="sk-你的key"
export DB_URL="jdbc:mysql://localhost:3306/fitness_agent?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false"
export DB_USERNAME="root"
export DB_PASSWORD="你的密码"

# 2. 后台运行
nohup java -jar fitness-training-agent-1.0.0.jar > app.log 2>&1 &

# 3. 用 Nginx 反向代理到 8080，即可通过域名访问（二维码自动用当前访问地址）
```

---

## 🔌 REST 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/task/submit` | 提交健身需求，返回 `sessionId` |
| GET | `/api/task/{sessionId}` | 查询进度（前端轮询） |
| GET | `/api/task/{sessionId}/result` | 获取完整训练方案（创建者视角） |
| GET | `/api/task/{sessionId}/share` | 分享只读视图（不含个人信息） |

提交示例：

```bash
curl -X POST http://localhost:8080/api/task/submit \
  -H "Content-Type: application/json" \
  -d '{"message":"我身高175cm，体重90kg，男，膝盖有旧伤不能深蹲和跑步，家里有哑铃和弹力带，每周能练4天，目标减脂，每次训练45分钟以内。"}'
```

---

## 🧪 测试用例

**输入：**

> 我身高 175cm，体重 90kg，男，膝盖有旧伤不能深蹲和跑步，家里有哑铃和弹力带，每周能练 4 天，目标减脂，每次训练 45 分钟以内。

**预期链路：**

1. IntentExtractor 提取：`heightCm=175`、`weightKg=90`、`gender=男`、`injuries=["膝伤"]`、`equipment=["哑铃","弹力带"]`、`daysPerWeek=4`、`goal=减脂`、`durationMinutes=45`。
2. RAG 检索：**排除**所有标记「膝伤」的动作（深蹲、弓步、开合跳、波比跳、高抬腿、跳绳、跑步等）以及杠铃/器械类动作，保留臀桥、靠墙静蹲、站姿提踵、平板支撑、卷腹、哑铃推举等膝友好动作。
3. Planner：可调用 `calculate_bmi`（BMI≈29.4 超重）、`recommend_calories`（减脂目标热量）计算目标，生成 4 天/周、每次 ≤45min 的减脂计划。
4. Critic：校验无禁忌动作、强度合理、负荷循序渐进。
5. Summarizer：输出含「训练安排 / 饮食建议 / 注意事项」的 Markdown 方案。

---

## ❓ 常见问题

- **启动报数据库连接失败**：先 `docker compose up -d` 启动 MySQL，确认端口 3306 未占用。
- **提交后一直「待解析/规划中」**：检查 `DEEPSEEK_API_KEY` 是否正确、能否访问 `https://api.deepseek.com`；查看日志 `com.fitness.agent`。
- **Redis 不可用**：项目已做容错，Redis 挂了会自动降级为纯 MySQL，不影响主链路。
- **想换模型 / 调温度**：改 `application.yml` 的 `deepseek.*` 或 `agent.*`；想改提示词直接改 `prompts.yml`。
- **新增工具**：实现 `AgentTool` 接口，在 `META-INF/services/com.fitness.agent.tool.AgentTool` 中登记即可被自动发现。

---

## 📄 免责声明

本项目用于技术演示与学习，内置训练计划与营养建议由大模型生成，**不构成医疗或专业健身建议**。有伤病或特殊健康状况者请在专业人士指导下训练。
