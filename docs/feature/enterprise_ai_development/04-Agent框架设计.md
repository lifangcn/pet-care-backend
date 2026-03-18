# Agent 框架设计文档

**版本**：v1.0
**创建时间**：2026-03-02
**作者**：Michael
**状态**：待评审

---

## 项目背景

**项目名称**：pet-care-ai
**技术栈**：Spring AI + 智谱AI + Elasticsearch + Redis
**当前架构**：单步 Function Calling，无法多步推理
**本文档目标**：引入 LangChain4j，实现 ReAct 多步推理 Agent

---

## 一、现状分析

### 1.1 当前架构

你的项目使用的是**单步 Function Calling**：

```
用户提问 → LLM 判断 → 调用单个 Tool → 返回结果
```

**代码位置**：`MultiIndexSearchTool.java:46-77`

### 1.2 存在的问题

| 问题 | 示例 | 影响 |
|------|------|------|
| 无法多步推理 | "周末北京有什么宠物活动？评价好的" | 只能检索，无法二次筛选 |
| 无法任务规划 | "帮我预约明天的疫苗接种提醒" | 需要查活动时间+设置提醒 |
| 无法自主决策 | 不知道先检索知识还是查活动 | 依赖固定逻辑 |
| 无错误恢复 | 工具调用失败后无法重试/降级 | 用户体验差 |

### 1.3 企业级场景需求

| 场景 | 需要 |
|------|------|
| 复杂问答 | 多步推理、工具组合 |
| 任务执行 | 规划、执行、验证 |
| 知识检索 | 混合检索、重排序 |
| 错误处理 | 重试、降级、回退 |

---

## 二、架构设计

### 2.1 LangChain4j 架构

```
┌─────────────────────────────────────────────────────────────┐
│                      ChatController                         │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   AgentOrchestrator                         │
│  1. 选择 Agent 类型（根据任务复杂度）                        │
│  2. 初始化 Agent 上下文                                      │
│  3. 执行 Agent 循环                                         │
└─────────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┴───────────────┐
            ▼                               ▼
┌──────────────────────┐        ┌──────────────────────┐
│   Simple Agent       │        │   ReAct Agent        │
│   （单步工具调用）     │        │   （多步推理）         │
│  - 直接返回结果        │        │  - Thought → Action  │
│  - 用于简单问题        │        │  - 多轮迭代          │
└──────────────────────┘        └──────────────────────┘
            │                               │
            └───────────────┬───────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      Tool Layer                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ Knowledge    │  │ Post Search  │  │ Reminder     │     │
│  │ Search       │  │              │  │ Tool         │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Agent 类型选择

| Agent 类型 | 适用场景 | 示例 |
|------------|----------|------|
| **Simple Agent** | 单步工具调用 | "查一下知识库" |
| **ReAct Agent** | 多步推理 | "周末北京有什么宠物活动？评价好的" |
| **Plan-and-Execute Agent** | 复杂任务规划 | "帮我安排下周的宠物护理计划" |

### 2.3 ReAct 循环

```
┌─────────────────────────────────────────────────────────────┐
│                         ReAct Loop                          │
│                                                              │
│  ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐ │
│  │ Thought │ → │ Action  │ → │ Observation│ → │  Final  │ │
│  │ (思考)  │    │ (调用工具)│  │  (观察结果) │ │ (回答)  │ │
│  └─────────┘    └─────────┘    └─────────┘    └─────────┘ │
│       │              │              │              │       │
│       ▼              ▼              ▼              ▼       │
│  "需要先检索"   searchActivities  "找到3个活动"   综合回答   │
│  "再查评价"     searchPosts      "活动评价不错"            │
│                                                              │
│  最大迭代次数：5 次                                          │
│  超时时间：30 秒                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、核心组件

### 3.1 AgentOrchestrator

**职责**：Agent 调度器，选择合适的 Agent 类型

**决策逻辑**：
```java
// 伪代码
public Agent selectAgent(String userQuery) {
    // 1. 意图识别
    Intent intent = intentClassifier.classify(userQuery);

    // 2. 根据意图选择 Agent
    if (intent.isSimpleQuery()) {
        return simpleAgent;
    } else if (intent.requiresMultiStep()) {
        return reactAgent;
    } else if (intent.requiresPlanning()) {
        return planAndExecuteAgent;
    }

    return simpleAgent;  // 默认
}
```

### 3.2 ReAct Agent

**职责**：多步推理 Agent

**核心方法**：
```java
public class ReActAgent {
    private List<Tool> tools;
    private ChatMemory memory;
    private int maxIterations = 5;

    public String execute(String query) {
        for (int i = 0; i < maxIterations; i++) {
            // 1. 生成 Thought
            String thought = generateThought(query, memory);

            // 2. 决定 Action
            Action action = decideAction(thought);

            // 3. 如果是最终答案，返回
            if (action.isFinalAnswer()) {
                return action.getContent();
            }

            // 4. 执行工具
            ToolResult result = executeTool(action);

            // 5. 观察
            String observation = formatObservation(result);

            // 6. 更新记忆
            memory.add(Message.user(observation));
        }

        return "未能完成请求";
    }
}
```

### 3.3 Tool 规范

**现有工具兼容**：
- `ReminderTool` → 包装为 LangChain4j Tool
- `MultiIndexSearchTool` → 包装为 LangChain4j Tool

**新增能力**：
```java
@Tool("计算日期范围，例如'周末'转换为具体日期")
public DateRangeResult calculateDateRange(String naturalLanguage) {
    // 解析"周末"、"下周"等自然语言
}

@Tool("根据用户评价对结果重排序")
public List<SearchResult> rerankByRating(List<SearchResult> results) {
    // 调用重排序模型
}
```

---

## 四、数据结构

### 4.1 Agent 执行记录

**索引名**：`agent_execution`

**Mapping**：
```json
{
  "mappings": {
    "properties": {
      "execution_id": {"type": "keyword"},
      "agent_type": {"type": "keyword"},
      "conversation_id": {"type": "keyword"},
      "user_id": {"type": "long"},
      "query": {"type": "text"},

      "steps": {
        "type": "nested",
        "properties": {
          "step_number": {"type": "integer"},
          "thought": {"type": "text"},
          "action": {"type": "keyword"},
          "tool_name": {"type": "keyword"},
          "tool_input": {"type": "text"},
          "observation": {"type": "text"},
          "duration_ms": {"type": "integer"}
        }
      },

      "result": {
        "type": "object",
        "properties": {
          "final_answer": {"type": "text"},
          "success": {"type": "boolean"},
          "reason": {"type": "text"}
        }
      },

      "metrics": {
        "type": "object",
        "properties": {
          "total_steps": {"type": "integer"},
          "total_duration_ms": {"type": "integer"},
          "tool_calls": {"type": "integer"}
        }
      },

      "created_at": {"type": "date"}
    }
  }
}
```

### 4.2 Prompt 模板

**ReAct Prompt**：
```
你是一个宠物关怀咨询顾问，可以使用以下工具回答用户问题：

可用工具：
{tools}

请按照以下格式思考：

Thought: [你对当前情况的思考]
Action: [要执行的工具名称]
Action Input: [工具的输入参数]
Observation: [工具返回的结果]

（可以重复 Thought-Action-Observation 多次）

当你获得足够信息后，使用以下格式给出最终答案：
Thought: [你已经获得足够信息]
Final Answer: [最终答案]

开始！

用户问题：{query}

Thought:
```

---

## 五、集成方式

### 5.1 依赖引入

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.29.1</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-zhipu-ai</artifactId>
    <version>0.29.1</version>
</dependency>
```

### 5.2 新增组件

```
pvt.mktech.petcare.agent
├── orchestrator
│   └── AgentOrchestrator           # Agent 调度器
├── agent
│   ├── SimpleAgent                 # 简单 Agent
│   ├── ReActAgent                  # ReAct Agent
│   └── PlanAndExecuteAgent         # 规划 Agent
├── tool
│   ├── LangChain4jToolWrapper      # 现有工具包装器
│   ├── DateRangeTool               # 日期计算工具
│   └── RerankTool                  # 重排序工具
├── memory
│   └── AgentMemory                 # Agent 记忆管理
├── prompt
│   └── PromptTemplateLoader        # Prompt 模板加载
└── repository
    └── AgentExecutionRepository    # 执行记录存储
```

### 5.3 ChatController 改造

```java
@GetMapping("/ai/chat/agent")
public Flux<String> agentChat(
        @RequestParam("message") String message,
        @RequestParam(value = "sessionId", required = false) String sessionId) {

    Long userId = UserContext.getUserId();
    String conversationId = conversationIdGenerator.generate(userId, sessionId);

    // 选择 Agent
    Agent agent = agentOrchestrator.selectAgent(message);

    // 执行
    return agent.executeStreaming(message, AgentContext.builder()
            .userId(userId)
            .conversationId(conversationId)
            .build());
}
```

### 5.4 配置调整

**application.yml 新增**：
```yaml
spring:
  ai:
    agent:
      enabled: true
      default-type: REACT  # SIMPLE / REACT / PLAN_AND_EXECUTE
      max-iterations: 5
      timeout-ms: 30000
      prompt-template: react/default
```

---

## 六、使用场景示例

### 6.1 场景1：多步查询

**用户问题**："周末北京有什么宠物活动？评价好的"

**ReAct 执行过程**：
```
Thought: 用户问周末的宠物活动，需要先确定时间范围
Action: calculateDateRange
Action Input: "这个周末"
Observation: 2026-03-08 至 2026-03-09

Thought: 有了时间范围，可以检索活动了
Action: searchActivities
Action Input: {"query": "宠物活动", "startTime": "2026-03-08", "endTime": "2026-03-09"}
Observation: 找到3个活动：[...]

Thought: 用户还要求评价好，需要查看这些活动的相关评价
Action: searchPosts
Action Input: {"query": "北京宠物活动 评价"}
Observation: 找到5条评价：[...]

Thought: 已经有足够信息，可以给出最终答案
Final Answer: 这个周末北京有以下宠物活动，根据评价推荐：
1. XXX（评分4.8，有50人参加）
2. YYY（评分4.5，免费参加）
...
```

### 6.2 场景2：任务规划

**用户问题**："帮我安排下周的宠物护理计划"

**Plan-and-Execute 执行过程**：
```
Thought: 这是一个复杂的规划任务，需要先制定计划
Action: createPlan
Action Input: {
  "goal": "安排下周宠物护理",
  "constraints": ["时间范围：下周", "对象：宠物"]
}
Observation: 计划已创建：
1. 检索宠物护理知识
2. 查询是否有疫苗/体检到期
3. 查询下周活动安排
4. 综合生成护理计划

Thought: 执行计划步骤1：检索宠物护理知识
Action: searchKnowledge
Action Input: {"query": "宠物护理计划 每周"}
...

Thought: 执行计划步骤2：查询疫苗记录
Action: queryPetRecords
Action Input: {"type": "vaccination", "range": "next_week"}
...

Thought: 所有步骤完成，生成最终计划
Final Answer: 已为您生成下周宠物护理计划：
...
```

---

## 七、错误处理

### 7.1 重试机制

| 场景 | 处理 |
|------|------|
| 工具调用失败 | 重试 1 次，参数调整 |
| 超时 | 返回部分结果 + 说明 |
| 迭代超限 | 总结已有信息，给出最佳答案 |

### 7.2 降级策略

```java
public class AgentOrchestrator {
    public String execute(String query) {
        try {
            return executeAgent(query);
        } catch (AgentException e) {
            // 降级到简单模式
            log.warn("Agent 执行失败，降级到简单模式", e);
            return fallbackToSimple(query);
        }
    }
}
```

---

## 八、风险点

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 循环不收敛 | 消耗 Token | 设置最大迭代次数 |
| 工具调用过多 | 成本增加 | 设置工具调用上限 |
| 复杂度增加 | 维护成本 | 充分测试 + 日志记录 |
| 响应慢 | 体验下降 | 流式返回 + 超时控制 |

---

## 九、实施计划

### 9.1 阶段1：基础框架

1. 引入 LangChain4j
2. 实现 AgentOrchestrator
3. 现有工具包装为 LangChain4j Tool
4. 支持 Simple Agent

### 9.2 阶段2：ReAct Agent

1. 实现 ReAct 循环
2. 设计 Prompt 模板
3. 添加重试/降级逻辑
4. 流式返回支持

### 9.3 阶段3：高级能力

1. Plan-and-Execute Agent
2. 意图识别优化
3. 工具调用优化（批处理、并行）
4. 评估与优化

---

## 十、会话管理接口（前端配套）

当前前端会话管理功能仅做本地模拟，需要后端补充以下接口以实现完整功能。

### 10.1 创建会话

**请求**
```
POST /ai/chat/session
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**请求体**
```json
{
  "name": "关于金毛的咨询"  // 可选，不传则使用默认名称"新对话"
}
```

**响应**
```json
{
  "id": "session_1234567890",
  "name": "关于金毛的咨询",
  "createdAt": "2026-03-02T10:00:00",
  "updatedAt": "2026-03-02T10:00:00"
}
```

**业务逻辑**
- 生成唯一 session_id（使用 UUID 或雪花算法）
- 持久化到数据库或 ES
- 返回会话信息

### 10.2 获取会话列表

**请求**
```
GET /ai/chat/sessions?page=0&size=20
Authorization: Bearer {accessToken}
```

**响应**
```json
{
  "total": 15,
  "items": [
    {
      "id": "session_1",
      "name": "关于金毛的咨询",
      "createdAt": "2026-03-01T10:00:00",
      "updatedAt": "2026-03-02T11:00:00",
      "messageCount": 8
    },
    {
      "id": "session_2",
      "name": "新对话",
      "createdAt": "2026-03-02T09:00:00",
      "updatedAt": "2026-03-02T09:05:00",
      "messageCount": 2
    }
  ]
}
```

**业务逻辑**
- 从 ES 的 `chat_history` 索引聚合当前用户的会话
- 按 `updatedAt` 倒序排列
- 支持分页

### 10.3 获取会话历史消息

**请求**
```
GET /ai/chat/session/{sessionId}/messages
Authorization: Bearer {accessToken}
```

**响应**
```json
[
  {
    "id": "msg_1",
    "role": "user",
    "content": "我家金毛最近不爱吃饭",
    "timestamp": "2026-03-02T10:00:00"
  },
  {
    "id": "msg_2",
    "role": "assistant",
    "content": "狗狗食欲不振可能的原因有...",
    "timestamp": "2026-03-02T10:00:05"
  }
]
```

**业务逻辑**
- 从 ES 查询该 session_id 的所有消息
- 按时间正序排列
- 必须验证当前用户是否有权访问该会话

### 10.4 删除会话

**请求**
```
DELETE /ai/chat/session/{sessionId}
Authorization: Bearer {accessToken}
```

**响应**
```
204 No Content
```

**业务逻辑**
- 验证会话归属（只能删除自己的）
- 删除 ES 中该 session_id 的所有消息
- 清除 Redis 中相关记忆

### 10.5 数据库表设计建议

#### chat_sessions 表（可选，或用 ES 替代）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | varchar(64) | PK |
| user_id | bigint | 用户 ID |
| name | varchar(200) | 会话名称 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

#### chat_history 索引（ES）

需确保包含 `session_id` 字段（已在文档 01 中定义）。

---

## 十一、前端改动

### 11.1 思考过程展示（可选）

**功能**：向用户展示 Agent 的思考过程

**示例展示**：
```
用户：周末北京有什么宠物活动？

AI 正在思考...
  Thought: 需要先确定"周末"的具体日期
  Action: 计算日期范围 → 2026-03-08 至 2026-03-09

  Thought: 有了时间范围，检索宠物活动
  Action: 搜索活动 → 找到3个活动

  Thought: 用户还要求评价好，需要查看相关评价
  Action: 搜索评价 → 找到5条评价

周末北京有以下宠物活动推荐：
1. 朝阳公园狗狗聚会（评分4.8）...
2. ...
```

### 11.2 实现方式

**后端 SSE 推送**：
```java
@GetMapping("/ai/chat/agent")
public Flux<ServerSentEvent<String>> agentChat(...) {
    return agent.executeStreaming(query)
        .map(step -> ServerSentEvent.builder(step)
            .event("thought")   // 思考步骤
            .event("action")    // 工具调用
            .event("answer")    // 最终答案
            .build());
}
```

**前端接收**：
```typescript
const eventSource = new EventSource('/ai/chat/agent?message=...');

eventSource.addEventListener('thought', (e) => {
  // 展示思考过程（灰色、折叠）
  showThought(e.data);
});

eventSource.addEventListener('action', (e) => {
  // 展示工具调用（蓝色图标）
  showAction(e.data);
});

eventSource.addEventListener('answer', (e) => {
  // 展示最终答案（正常显示）
  showAnswer(e.data);
});
```

### 11.3 界面建议

| 元素 | 样式 | 交互 |
|------|------|------|
| Thought | 灰色小字，默认折叠 | 点击展开 |
| Action | 蓝色图标 + 工具名 | 悬浮显示参数 |
| Answer | 正常聊天样式 | - |

### 11.4 优先级

**P2**（可选）：思考过程展示有助于用户理解 AI 行为，但不是必须功能。

---

**变更记录**：
| 日期 | 版本 | 变更内容 | 作者 |
|------|------|----------|------|
| 2026-03-02 | v1.0 | 初始版本 | Michael |
