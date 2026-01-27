# 向量数据库迁移方案：PgVector → Elasticsearch

## 1. 引入背景

### 1.1 现状
- **RAG 知识库**：使用 PgVector 存储文档向量（`tb_knowledge_document`）
- **业务数据**：MySQL 存储 Post、Label、PostLabel、Interaction 等社交数据
- **检索流程**：MySQL 模糊查询 → 提示词转换 → PgVector 向量检索 → 结果融合

### 1.2 痛点
- 串联查询响应时间长（≈110ms）
- 代码复杂度高，多次网络调用
- 无法实现高效的混合检索（关键词 + 向量联合打分）

### 1.3 目标
使用 Elasticsearch 统一向量存储与全文检索，实现混合检索一次查询返回综合排序结果。

---

## 2. 功能设计

### 2.1 索引设计

#### 2.1.1 知识库索引（knowledge_document）
```yaml
索引名: knowledge_document
Settings:
  number_of_shards: 3
  number_of_replicas: 1
  index.knn: true

Properties:
  id: { type: long }
  name: { type: text, analyzer: ik_max_word }
  file_url: { type: keyword }
  file_type: { type: keyword }
  content: { type: text, analyzer: ik_max_word }       # 分块后的文本内容
  embedding: { type: knn_vector, dimension: 1024 }    # text-embedding-v3
  chunk_index: { type: integer }                       # 分块序号
  version: { type: integer }
  status: { type: short }                              # 1-有效 0-禁用
  created_at: { type: date }
```

**数据结构说明**：
- `tb_knowledge_document`（MySQL）：仅存储文档元数据（id, name, file_url, file_type, status, version, created_at）
- ES 索引：存储文档分块内容及向量，每个分块作为独立文档
- 父子关系：通过 `parent_document_id` 字段关联同一文档的所有分块
- 向量化流程：文件上传 → Tika 解析 → 文本分块 → DashScope 向量化 → ES 入库

#### 2.1.2 动态索引（post）
```yaml
索引名: post
Settings:
  number_of_shards: 3
  number_of_replicas: 1
  index.knn: true

Properties:
  id: { type: long }
  user_id: { type: long }
  title: { type: text, analyzer: ik_max_word }
  content: { type: text, analyzer: ik_max_word }
  post_type: { type: short }                          # 1-好物分享 2-服务推荐 3-地点推荐 4-日常分享 5-活动打卡 6-活动报名
  media_urls: { type: flattened }                      # 图片/视频URL数组
  external_link: { type: keyword }
  location: {
    type: object,
    properties: {
      name: { type: text },
      address: { type: text },
      latitude: { type: double },
      longitude: { type: double }
    }
  }
  price_range: { type: keyword }
  like_count: { type: integer }
  rating_avg: { type: float }
  view_count: { type: integer }
  status: { type: short }                             # 1-正常 2-隐藏
  activity_id: { type: long }
  labels: { type: keyword }                           # 标签名称数组（冗余存储，避免二次查询）
  embedding: { type: knn_vector, dimension: 1024 }    # title + content + label_names 的向量
  created_at: { type: date }
```

#### 2.1.3 标签索引（label）
```yaml
索引名: label
Settings:
  number_of_shards: 1
  number_of_replicas: 1

Properties:
  id: { type: long }
  name: { type: text, fields: { keyword: { type: keyword } } }
  type: { type: short }                               # 1-通用 2-宠物品种 3-内容类型
  icon: { type: keyword }
  color: { type: keyword }
  use_count: { type: integer }
  is_recommended: { type: short }
  status: { type: short }
  created_at: { type: date }
```

### 2.2 数据同步方案

#### 2.2.1 CDC 同步管道
```yaml
Pipeline: MySQL → Debezium → Kafka → Elasticsearch

监听表范围:
  - tb_post: 动态增删改
  - tb_post_label: 动态标签关联变更 → 触发对应 Post 的 ES 更新
  - tb_label: 标签名称变更 → 批量更新所有关联的 Post 文档
  - tb_interaction: 互动数据 → 更新 Post 计数字段（like_count, view_count）
  - tb_knowledge_document: 知识文档变更

Topics (Debezium 自动创建):
  - petcare_mysql.pet_care_core.tb_post: 动态变更（create/update/delete）
  - petcare_mysql.pet_care_core.tb_post_label: 标签关联变更
  - petcare_mysql.pet_care_core.tb_label: 标签元数据变更
  - petcare_mysql.pet_care_core.tb_interaction: 互动数据变更
  - petcare_mysql.pet_care_ai.tb_knowledge_document: 知识文档变更

Debezium 消息格式:
  - op: c=create/r=read/u=update/d=delete
  - before: 变更前数据
  - after: 变更后数据
  - source: 数据库、表、时间戳等元信息

数据聚合策略:
  - Kafka 消费者监听到变更后，立即聚合数据
  - Post 变更：关联查询 tb_label 获取标签名称数组
  - Label 变更：查询所有关联的 Post ID，批量更新 ES 文档
  - Interaction 变更：只更新对应 Post 的计数字段（使用 partial update）

Consumer Groups:
  - es-sync-group: ES 同步消费者
```

#### 2.2.2 向量化策略
```yaml
Post 向量生成:
  输入: title + content + label_names(聚合后的标签名称)
  模型: DashScope text-embedding-v3 (1024维)
  触发: 动态发布/更新时异步处理
  存储: ES embedding 字段

知识库向量:
  元数据存储: tb_knowledge_document 只存储文档元数据（文件URL、类型、状态等）
  内容处理: 文档上传 → Tika 解析 → 文本分块 → 向量化 → ES 入库
  索引结构: 每个文档分块作为独立的 ES 文档，通过父文档 ID 关联
  模型: DashScope text-embedding-v3
```

### 2.3 混合检索算法

#### 2.3.1 打分公式
```text
Final_Score =
  α * BM25_Score +                    # 关键词匹配
  β * Vector_Similarity +             # 向量相似度
  γ * Engagement_Score                # 互动权重

参数:
  α = 0.4                             # 关键词权重
  β = 0.5                             # 向量权重
  γ = 0.1                             # 互动权重

Engagement_Score =
  (like_count * 1.0 +
   view_count * 0.1 +
   rating_avg * rating_count * 2.0) /
  log(age_hours + 2)                  # 时间衰减
```

#### 2.3.2 查询DSL示例
```json
{
  "query": {
    "bool": {
      "should": [
        {
          "knn": {
            "field": "embedding",
            "query_vector": [1024维向量],
            "k": 50,
            "num_candidates": 100
          }
        },
        {
          "multi_match": {
            "query": "关键词",
            "fields": ["title^2", "content"],
            "type": "best_fields"
          }
        }
      ]
    }
  },
  "size": 20,
  "rank": {
    "type": "rms",
    "ranks": [
      { "score": { "query": { "knn": {} } }, "weight": 0.5 },
      { "score": { "query": { "multi_match": {} } }, "weight": 0.4 },
      { "script_score": { "script": "..." }, "weight": 0.1 }
    ]
  }
}
```

---

## 3. 技术架构

### 3.1 架构图
```
┌─────────────────────────────────────────────────────────────────┐
│                           应用层                                 │
├─────────────────────────────────────────────────────────────────┤
│  pet-care-gateway  │  pet-care-core  │  pet-care-ai             │
├─────────────────────────────────────────────────────────────────┤
│                        Spring AI 抽象层                          │
│  VectorStore 接口 │ EmbeddingModel │ RetrievalAugmentor          │
├─────────────────────────────────────────────────────────────────┤
│                           同步层                                 │
│  Kafka Producer │ CDC Event │ 异步向量化                          │
├─────────────────────────────────────────────────────────────────┤
│                        存储层 (目标)                              │
│  Elasticsearch 8.x (KNN Plugin)                                 │
│  - knowledge_document 索引                                       │
│  - post 索引                                                     │
│  - label 索引                                                    │
├─────────────────────────────────────────────────────────────────┤
│                        存储层 (源)                               │
│  MySQL │ PgVector(逐步废弃) │ Kafka                              │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 技术选型
| 组件 | 选型                      | 版本                | 说明 |
|------|-------------------------|-------------------|------|
| 搜索引擎 | Elasticsearch           | 8.17              | 支持 KNN 向量检索 |
| 分词器 | IK Analysis             | 8.17              | 中文分词 |
| 同步中间件 | Kafka                   | 7.7.7             | CDC 事件流 |
| CDC 连接器 | Debezium                | 2.5               | MySQL Binlog 捕获 |
| 向量模型 | DashScope               | text-embedding-v3 | 阿里云 1024维 |
| Spring AI | spring-ai-elasticsearch | 1.0.1             | 向量存储抽象 |

---

## 4. 实施步骤

### 4.1 阶段一：基础设施（1周）
```yaml
任务:
  1. 部署 Elasticsearch 集群
  2. 安装 KNN Plugin、IK Analysis
  3. 配置 Kafka Topic
  4. 部署 Debezium Connect
  5. 注册 MySQL Connector

验收:
  - ES 集群健康状态 green
  - KNN 插件可用
  - Kafka 连接正常
  - Debezium Connector 状态 RUNNING
```

### 4.2 阶段二：索引与映射（3天）
```yaml
任务:
  1. 创建 knowledge_document 索引
  2. 创建 post、label 索引
  3. 编写索引管理工具类

验收:
  - 索引映射验证通过
  - 测试数据可正常写入
```

### 4.3 阶段三：向量服务抽象（1周）
```yaml
任务:
  1. 定义 VectorStoreService 接口
  2. 实现 ElasticsearchVectorStore
  3. 适配 Spring AI VectorStore 接口
  4. 配置双写（PgVector + ES）过渡期

验收:
  - 单元测试通过
  - 与现有代码兼容
```

### 4.4 阶段四：数据同步（1周）

#### 4.4.1 实施流程

**步骤1：全量数据迁移**

1. **Post 全量迁移**
   - 从 `tb_post` 表分页读取所有动态数据
   - 关联查询 `tb_post_label` 和 `tb_label` 获取标签名称数组
   - 关联查询 `tb_interaction` 聚合 `like_count`、`view_count`、`rating_avg`
   - 调用 DashScope API 生成 embedding（批量处理，每批10条）
   - 批量写入 ES `post` 索引

2. **Label 全量迁移**
   - 从 `tb_label` 表读取所有标签
   - 批量写入 ES `label` 索引

3. **Activity 全量迁移**
   - 从 `tb_activity` 表读取所有活动
   - 批量写入 ES `activity` 索引

**步骤2：增量 CDC 同步**

1. **Debezium Connector 配置**
   - 注册 MySQL Connector 监听表：`tb_post`, `tb_post_label`, `tb_label`, `tb_interaction`, `tb_knowledge_document`
   - 配置 Transform：`ExtractNewRecordState` 提取变更后数据
   - Topic 自动创建：`petcare_mysql.{database}.{table}`

2. **Kafka 消费者（ES Sync Service）**
   - 消费 Debezium Topic
   - 根据 `source.table` 字段分发到不同处理器：
     - `tb_post` → PostUpdateHandler（同步删除旧向量，重新生成向量并入库）
     - `tb_post_label` → PostLabelUpdateHandler（重新聚合标签，更新 Post 文档）
     - `tb_label` → LabelUpdateHandler（批量更新所有关联 Post 的 labels 字段）
     - `tb_interaction` → InteractionUpdateHandler（更新 Post 的计数字段）
     - `tb_knowledge_document` → KnowledgeDocumentHandler（同步知识文档变更）

3. **异步向量化服务**
   - 创建独立的向量化线程池
   - 接收 CDC 事件，提取需要向量化的内容
   - 调用 DashScope API 生成向量
   - 更新 ES 文档的 embedding 字段

**步骤3：数据一致性校验**

1. **实时校验**
   - 定时任务每小时对比 MySQL 和 ES 的数据条数
   - 发现不一致时记录日志并告警

2. **全量校验**
   - 每日凌晨执行全量比对
   - 以 MySQL 为基准，补齐缺失数据

#### 4.4.2 核心代码结构

```
pet-care-ai/
├── consumer/                     # Kafka 消费者
│   ├── DebeziumKafkaConsumer.java # Debezium 消费入口
│   └── handler/                  # 事件处理器
│       ├── PostUpdateHandler.java
│       ├── PostLabelUpdateHandler.java
│       ├── LabelUpdateHandler.java
│       ├── InteractionUpdateHandler.java
│       └── KnowledgeDocumentHandler.java
├── service/
│   ├── DataMigrationService.java # 全量迁移服务
│   └── VectorizationService.java # 向量化服务
└── config/
    └── KafkaConsumerConfig.java  # Kafka 消费者配置
```

#### 4.4.3 关键实现点

1. **Post 变更处理流程**
   ```
   Debezium → Kafka → PostUpdateHandler
   → 1. 解析 Debezium 消息（op=after 数据）
   → 2. 查询 MySQL 聚合完整数据（含标签、互动数据）
   → 3. 提取文本内容：title + content + label_names
   → 4. 调用向量化服务生成 embedding
   → 5. 更新 ES 文档（upsert）
   ```

2. **标签关联变更处理**
   ```
   PostLabelUpdateHandler
   → 1. 解析 Debezium 消息获取 post_id 和 label_id
   → 2. 查询 tb_post_label 获取该动态的所有标签
   → 3. 查询 tb_label 获取标签名称
   → 4. 更新 ES 中对应 Post 的 labels 字段
   → 5. 触发重新向量化（标签名称纳入向量生成）
   ```

3. **互动数据更新**
   ```
   InteractionUpdateHandler
   → 1. 解析 Debezium 消息获取 target_id 和 interaction_type
   → 2. 聚合查询计算新的 like_count, view_count, rating_avg
   → 3. 使用 ES partial update 仅更新计数字段
   ```

#### 4.4.4 验收标准
- 数据条数一致（MySQL vs ES）
- 向量相似度测试通过
- CDC 延迟 < 2s

### 4.5 阶段五：混合检索（1周）

#### 4.5.1 实施流程

**步骤1：混合检索 DSL 封装**

1. **创建 HybridSearchService**
   - 封装 ES KNN 查询 + BM25 查询
   - 实现打分融合逻辑
   - 支持分页、排序、过滤

2. **实现查询构建器**
   - `HybridSearchQueryBuilder`：构建 ES bool 查询
   - `ScoreFunctionBuilder`：构建打分脚本
   - 支持动态调整权重参数

3. **定义打分公式**
   ```text
   Final_Score = α * BM25_Score + β * Vector_Similarity + γ * Engagement_Score

   参数：
   α = 0.4  # 关键词匹配权重
   β = 0.5  # 向量相似度权重
   γ = 0.1  # 互动权重

   Engagement_Score = (like_count * 1.0 + view_count * 0.1) / log(age_hours + 2)
   ```

**步骤2：优化打分权重参数**

1. **离线评估**
   - 准备测试数据集（query + 相关文档）
   - 计算不同权重组合下的 NDCG、MRR
   - 选择最优参数组合

2. **在线 A/B 测试**
   - 创建实验组（混合检索）和对照组（纯关键词）
   - 对比 CTR、转化率
   - 逐步调整权重

**步骤3：性能测试与调优**

1. **索引优化**
   - 调整 `number_of_shards` 和 `number_of_replicas`
   - 优化 `_source` 过滤，只返回必要字段
   - 使用 `search_after` 替代深分页

2. **查询优化**
   - 控制 KNN 候选数量 `num_candidates`
   - 限制向量检索的 `k` 值
   - 使用 filter 过滤减少计算量

**步骤4：A/B 测试**

1. **指标定义**
   - CTR（点击率）
   - 转化率
   - 用户停留时间
   - 搜索零结果率

2. **实验设计**
   - 流量分配：50% 对照组，50% 实验组
   - 实验周期：7 天
   - 显著性检验：p-value < 0.05

#### 4.5.2 核心代码结构

```
pet-care-ai/
├── service/
│   ├── HybridSearchService.java          # 混合检索服务
│   └── SearchParameterService.java       # 搜索参数配置服务
├── builder/
│   ├── HybridSearchQueryBuilder.java     # 查询构建器
│   └── ScoreFunctionBuilder.java         # 打分函数构建器
├── domain/
│   ├── SearchRequest.java                # 搜索请求
│   ├── SearchResponse.java               # 搜索响应
│   └── SearchHit.java                    # 搜索结果
└── config/
    └── SearchConfig.java                 # 搜索配置（权重参数）
```

#### 4.5.3 关键实现点

1. **混合检索查询 DSL**
   ```json
   {
     "query": {
       "bool": {
         "should": [
           {
             "knn": {
               "field": "embedding",
               "query_vector": "[向量]",
               "k": 50,
               "num_candidates": 100
             }
           },
           {
             "multi_match": {
               "query": "[关键词]",
               "fields": ["title^2", "content"]
             }
           }
         ]
       }
     },
     "size": 20,
     "rank": {
       "type": "rms",
       "ranks": [
         {"score": {"query": {"knn": {}}}, "weight": 0.5},
         {"score": {"query": {"multi_match": {}}}, "weight": 0.4},
         {"script_score": {"script": "..."}, "weight": 0.1}
       ]
     }
   }
   ```

2. **权重参数动态配置**
   - 将 α、β、γ 参数写入配置中心或数据库
   - 支持运行时调整，无需重启服务
   - 记录每次调整的参数和效果

3. **缓存优化**
   - 热门查询结果缓存（Redis）
   - 向量缓存（相同 query 复用向量）
   - 过滤条件缓存（标签、地点等）

#### 4.5.4 验收标准
- 响应时间 P99 < 100ms
- 推荐效果提升 10%+（CTR 对比）
- 搜索零结果率下降 20%

### 4.6 阶段六：灰度与切换（1周）
```yaml
任务:
  1. 灰度流量 10% → 50% → 100%
  2. 监控指标观察
  3. PgVector 下线

验收:
  - 线上稳定运行 7 天
  - 无重大 bug
```

---

## 5. 资源评估

### 5.1 ES 集群配置
```yaml
初期（3个月）:
  节点数: 3（2 data + 1 master）
  规格: 8C 16G
  存储: 500GB SSD

预估数据量:
  Post: 10万 × 5KB ≈ 500MB
  知识库分块: 50万 × 3KB ≈ 1.5GB
  向量: 60万 × 4KB ≈ 2.4GB
  总计: ≈5GB（含副本）
```

### 5.2 向量化成本
```yaml
DashScope text-embedding-v3:
  价格: ¥0.0007/千tokens
  预估: 60万文档 × 500tokens ≈ 30万tokens
  成本: ≈ ¥210（一次性）

每月增量:
  新增文档: 1万 × 500tokens = 500万tokens
  月成本: ≈ ¥3.5
```

---

## 6. 数据一致性保障

### 6.1 同步策略
```yaml
全量同步:
  方式: 定时任务 + 批量写入
  频率: 仅初始化时执行

增量同步:
  方式: Debezium CDC
  延迟: < 2s
  顺序: 保证同一文档内有序

异常处理:
  - 死信队列: 记录失败事件
  - 重试策略: 指数退避
  - 告警: 失败率 > 1%
```

### 6.2 数据校验
```yaml
实时校验:
  - 每日抽样对比（MySQL vs ES）
  - 计数校验（每小时一次）

全量校验:
  - 每周凌晨全量比对
  - 不一致自动修复
```

---

## 7. 回滚方案

### 7.1 触发条件
```yaml
立即回滚:
  - ES 集群不可用 > 5min
  - 查询响应时间 > 500ms 持续 10min
  - 数据丢失风险

计划回滚:
  - 推荐效果下降 > 5%
  - 成本超预期 50%+
```

### 7.2 回滚步骤
```text
1. 切换读流量: ES → PgVector
2. 停止 CDC 同步
3. 保留 ES 数据（用于问题分析）
4. 修复后重新同步
```

---

## 8. 监控指标

### 8.1 系统指标
```yaml
ES 集群:
  - 集群健康状态
  - JVM 堆使用率 < 75%
  - CPU 使用率 < 70%
  - 查询 P99 < 100ms

同步管道:
  - Kafka 消费延迟 < 1s
  - CDC Lag < 1000 条
  - 失败率 < 0.1%
```

### 8.2 业务指标
```yaml
检索质量:
  - 点击率 CTR
  - 转化率
  - 用户满意度

数据质量:
  - 向量覆盖率 > 95%
  - 数据一致性 = 100%
```