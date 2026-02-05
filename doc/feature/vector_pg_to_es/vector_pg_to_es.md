# Elasticsearch 检索架构设计：知识库向量检索 + 业务数据全文检索

## 1. 引入背景

### 1.1 现状
- **RAG 知识库**：使用 PgVector 存储文档向量（`tb_knowledge_document`）
- **业务数据**：MySQL 存储 Post、Label、PostLabel、Interaction、Activity 等社交数据
- **检索流程**：MySQL 模糊查询 → 提示词转换 → PgVector 向量检索 → 结果融合

### 1.2 痛点
- 串联查询响应时间长
- 代码复杂度高，多次网络调用
- 无法实现高效的混合检索（关键词 + 向量联合打分）

### 1.3 目标
使用 Elasticsearch 统一向量存储与全文检索：
- **知识库**：KNN 向量检索（语义复杂的专业文档）
- **Post/Activity**：BM25 全文检索（关键词精确匹配）

---

## 2. 功能设计

### 2.1 索引设计

#### 2.1.1 知识库索引（knowledge_document）
```yaml
索引名: knowledge_document
Settings:
  number_of_shards: 3
  number_of_replicas: 1

Properties:
  id: { type: long }                                    # 分块唯一 ID
  parent_document_id: { type: long }                   # 关联 tb_knowledge_document.id
  name: { type: text, analyzer: ik_max_word }           # 文档名称
  content: { type: text, analyzer: ik_max_word }       # 分块内容
  embedding: { type: dense_vector, dims: 1024, similarity: cosine }  # 向量
  chunk_index: { type: integer }                       # 分块序号
  status: { type: short }                              # 1-有效 0-禁用
  created_at: { type: date }
```

**数据结构说明**：
- `tb_knowledge_document`（MySQL）：存储文档元数据（id, name, file_url, file_type, status 等）
- ES 索引：由上传接口直接写入，存储分块内容、向量、文档名称、状态
- 向量化流程：文件上传 → Tika 解析 → 文本分块 → DashScope 向量化 → ES 入库

#### 2.1.2 动态索引（post）
```yaml
索引名: post
Settings:
  number_of_shards: 3
  number_of_replicas: 1

Properties:
  id: { type: long }
  user_id: { type: long }
  title: { type: text, analyzer: ik_max_word }
  content: { type: text, analyzer: ik_max_word }
  post_type: { type: short }
  media_urls: { type: keyword }
  external_link: { type: keyword }
  location_name: { type: text }
  location_address: { type: text }
  location_latitude: { type: double }
  location_longitude: { type: double }
  price_range: { type: keyword }
  like_count: { type: integer }
  rating_avg: { type: float }
  view_count: { type: integer }
  status: { type: short }
  activity_id: { type: long }
  created_at: { type: date }
```

#### 2.1.3 活动索引（activity）
```yaml
索引名: activity
Settings:
  number_of_shards: 3
  number_of_replicas: 1

Properties:
  id: { type: long }
  user_id: { type: long }
  title: { type: text, analyzer: ik_max_word }
  description: { type: text, analyzer: ik_max_word }
  activity_type: { type: short }
  activity_time: { type: date }
  end_time: { type: date }
  address: { type: text, analyzer: ik_max_word }
  online_link: { type: keyword }
  max_participants: { type: integer }
  current_participants: { type: integer }
  status: { type: short }
  check_in_enabled: { type: short }
  check_in_count: { type: integer }
  created_at: { type: date }
```

### 2.2 数据同步方案

#### 2.2.1 CDC 同步管道
```yaml
Pipeline: MySQL → Debezium → Kafka → Spring Boot Consumer → Elasticsearch

监听表范围:
  - tb_post: 动态增删改
  - tb_activity: 活动增删改

Topics:
  - PET_CARE_CDC.pet_care_core.tb_post
  - PET_CARE_CDC.pet_care_core.tb_activity

数据处理:
  - Consumer 直接映射数据写 ES
  - 无需向量化
```

**知识库**：不上 CDC。文档上传/更新/删除由应用双写 MySQL（元数据）和 ES（分块+向量）。

#### 2.2.2 检索策略
```yaml
知识库检索:
  方式: KNN 向量检索
  输入: embedding 字段
  模型: DashScope text-embedding-v3 (1024维)

Post / Activity 检索:
  方式: BM25 全文检索
  字段: title^2, content (Post) / title^2, description, address (Activity)
  分词: IK 中文分词
```

### 2.3 检索算法

#### 2.3.1 知识库：KNN 向量检索
```json
{
  "query": {
    "knn": {
      "field": "embedding",
      "query_vector": [1024维向量],
      "k": 5,
      "num_candidates": 10
    }
  }
}
```

#### 2.3.2 Post/Activity：BM25 全文检索
```json
{
  "query": {
    "multi_match": {
      "query": "查询关键词",
      "fields": ["title^2", "content"],
      "type": "best_fields"
    }
  }
}
```

---

## 3. 技术架构

### 3.1 架构图
```
┌──────────────────────────────────────────────────────────────┐
│                        应用层                                │
│  pet-care-gateway │ pet-care-core │ pet-care-ai              │
├──────────────────────────────────────────────────────────────┤
│                      Spring AI 抽象层                         │
│  VectorStore 接口 │ EmbeddingModel │ Function Calling        │
├──────────────────────────────────────────────────────────────┤
│                        同步层                                 │
│  Kafka CDC（仅 tb_post、tb_activity）                         │
├──────────────────────────────────────────────────────────────┤
│                      存储层                                   │
│  Elasticsearch 8.x                                           │
│  - knowledge_document (向量检索)                             │
│  - post (BM25 检索)                                          │
│  - activity (BM25 检索)                                      │
└──────────────────────────────────────────────────────────────┘
```

### 3.2 技术选型
| 组件 | 选型 | 版本 | 说明 |
|------|------|------|------|
| 搜索引擎 | Elasticsearch | 8.17 | 支持向量 + 全文 |
| 分词器 | IK Analysis | 8.17 | 中文分词 |
| 同步中间件 | Kafka | 7.7.7 | CDC 事件流 |
| CDC 连接器 | Debezium | 2.5 | MySQL Binlog |
| 向量模型 | DashScope | text-embedding-v3 | 阿里云 1024维 |

---

## 4. 实施步骤

### 4.1 阶段一：基础设施（1周）
```yaml
任务:
  1. 部署 Elasticsearch 集群
  2. 安装 IK Analysis
  3. 配置 Kafka Topic
  4. 部署 Debezium Connect

验收:
  - ES 集群健康状态 green
  - Kafka 连接正常
```

### 4.2 阶段二：索引与映射（2天）
```yaml
任务:
  1. 创建 knowledge_document 索引（含 embedding）
  2. 创建 post、activity 索引（不含 embedding）
  3. 编写索引管理工具类
```

### 4.3 阶段三：数据同步（1周）

#### 4.3.1 实施流程

**全量数据迁移**：
- Post：从 `tb_post` 批量读取，直接写 ES
- Activity：从 `tb_activity` 批量读取，直接写 ES

**增量 CDC 同步**：
- 监听 `tb_post`、`tb_activity` 变更
- Consumer 解析 Debezium 消息，直接写 ES

**数据一致性校验**：
- 每小时对比 MySQL 和 ES 条数
- 每周全量比对

#### 4.3.2 核心代码结构
```
pet-care-ai/
├── sync/
│   ├── consumer/
│   │   ├── PostCdcListener.java
│   │   └── ActivityCdcListener.java
│   └── service/
│       └── DataMigrationService.java
└── config/
    └── KafkaConsumerConfig.java
```

### 4.4 阶段四：检索实现（1周）

```yaml
任务:
  1. 实现 MultiIndexSearchTool（AI Function Calling）
  2. 知识库：KNN 向量检索
  3. Post/Activity：BM25 全文检索

验收:
  - 响应时间 P99 < 100ms
```

---

## 5. 设计决策

### 5.1 核心决策
| 决策 | 说明 |
|------|------|
| Post/Activity 不向量化 | 关键词精确匹配已满足需求，降低成本 |
| 知识库保留向量检索 | 专业术语、语义复杂需要向量理解 |
| 同步仅监听 tb_post、tb_activity | 简化 CDC 链路 |
| 移除 Labels 索引 | 检索以内容为主，避免标签误导 |

### 5.2 检索方式选择理由
| 数据类型 | 检索方式 | 理由 |
|---------|---------|------|
| 知识库文档 | KNN 向量 | 专业术语多、语义复杂、长文本 |
| Post | BM25 全文 | 用户查询意图明确、实体匹配为主 |
| Activity | BM25 全文 | 时间地点等精确信息 |

---

## 6. 资源评估

### 6.1 ES 集群配置
```yaml
节点数: 3（2 data + 1 master）
规格: 8C 16G
存储: 300GB SSD

数据量:
  Post: 10万 × 4KB ≈ 400MB
  Activity: 1万 × 2KB ≈ 20MB
  知识库分块: 50万 × 3KB ≈ 1.5GB
  向量: 知识库 50万 ≈ 2GB
  总计: ≈4GB（含副本）
```

### 6.2 向量化成本
```yaml
DashScope text-embedding-v3: ¥0.0007/千tokens

知识库: 50万分块 × 500tokens ≈ ¥175（一次性）
Post/Activity: 无需向量化
```

---

## 7. 未来扩展

### 7.1 可选优化（6个月后评估）
- 如果发现语义查询需求增长
- 可对优质 Post/Activity 按需向量化
- 采用 BM25 + KNN 混合检索

### 7.2 评估指标
- 语义查询占比 > 20%
- 用户反馈召回率不足
- 业务规模增长需要更好体验