# Pet-Care-AI 企业级改造方案

**项目名称**：pet-care-ai
**创建时间**：2026-03-02
**作者**：Michael
**状态**：待评审

---

## 一、项目背景

### 1.1 当前架构

```
用户请求 → ChatController → Spring AI → 智谱AI → 响应
                    ↓
            Function Calling
            (ReminderTool, MultiIndexSearchTool)
```

**技术栈**：
- Spring AI + 智谱AI
- Elasticsearch（向量检索）
- Redis（会话记忆）

### 1.2 存在的问题

| 能力缺失 | 影响 |
|----------|------|
| 无 Agent 框架 | 无法多步推理、任务规划 |
| 无长期记忆 | 会话结束后无法复用 |
| 无评估体系 | 不知道 RAG 质量、工具调用准确率 |
| 无可观测性 | 问题排查困难，无法追踪链路 |

### 1.3 改造目标

将当前的单步 Function Calling 架构升级为企业级 AI 应用。

---

## 二、改造方案概览

### 2.1 四大模块

| 模块 | 方案 | 优先级 | 前端改动 |
|------|------|--------|----------|
| **记忆管理** | ES 语义检索 + Redis 会话窗口 | P1 | 清除历史记录 |
| **可观测性** | 结构化日志 + ELK | P0 | 无 |
| **评估体系** | RAGAS + 自埋点 | P2 | 无 |
| **Agent框架** | LangChain4j + ReAct | P3 | 思考过程展示（可选） |

### 2.2 实施顺序

```
1. 可观测性（P0）→ 基础，数据支撑
2. 记忆管理（P1）→ 用户价值明显
3. 评估体系（P2）→ 有了数据再优化
4. Agent框架（P3）→ 复杂度高，最后做
```

---

## 三、文档结构

```
doc/feature/enterprise_ai_development/
├── README.md                    # 本文档，总览
├── 01-记忆管理设计.md           # 长期语义记忆实现
├── 02-可观测性设计.md           # 链路追踪与监控
├── 03-评估体系设计.md           # RAG/工具调用评估
└── 04-Agent框架设计.md          # 多步推理 Agent
```

---

## 四、核心依赖

### 4.1 新增依赖

```xml
<!-- Agent 框架 -->
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

<!-- 评估框架 -->
<dependency>
    <groupId>com.github.raghas</groupId>
    <artifactId>ragas</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 4.2 现有依赖（复用）

- ElasticsearchClient（已有）
- EmbeddingModel（已有）
- Redis（已有）

---

## 五、前端改动汇总

| 文档 | 功能 | 优先级 |
|------|------|--------|
| 记忆管理 | 清除历史记录 API | P1 |
| Agent框架 | 思考过程展示（可选） | P2 |

### 5.1 API 列表

**清除历史记录**：
```
DELETE /ai/chat/history
Authorization: Bearer {token}
```

---

## 六、新对话快速开始

### 6.1 如果要实施某个模块

1. 阅读对应的设计文档
2. 查看文档中的"新增组件"章节
3. 参考"集成方式"章节进行编码

### 6.2 如果要了解整体架构

1. 阅读本文档（README.md）
2. 根据需要深入各模块文档

### 6.3 项目关键信息

| 项目 | 值 |
|------|-----|
| 包名前缀 | `pvt.mktech.petcare` |
| 向量维度 | 1024 |
| 相似度算法 | cosine |
| LLM | 智谱AI |
| 当前会话窗口 | 10 条消息 |

---

## 七、风险与注意事项

| 风险 | 缓解措施 |
|------|----------|
| ES 写入压力 | 异步写入 + 降级 |
| Token 消耗增加 | 监控 + 预算控制 |
| 架构复杂度 | 分阶段实施，充分测试 |
| 前端依赖 | 后端先行，前端可选 |

---

## 八、变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|----------|------|
| 2026-03-02 | v1.0 | 初始版本 | Michael |
