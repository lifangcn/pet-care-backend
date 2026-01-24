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
  labels: { type: keyword }                           # 标签ID数组
  embedding: { type: knn_vector, dimension: 1024 }    # title + content + labels 的向量
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

Topics:
  - petcare.post.create: 动态创建
  - petcare.post.update: 动态更新
  - petcare.post.delete: 动态删除
  - petcare.interaction.create: 互动创建（更新计数字段）
  - petcare.knowledge_document: 知识库变更

Consumer Groups:
  - es-sync-group: ES 同步消费者
```

#### 2.2.2 向量化策略
```yaml
Post 向量生成:
  输入: title + content + labels(聚合)
  模型: DashScope text-embedding-v3 (1024维)
  触发: 动态发布/更新时异步处理
  存储: ES embedding 字段

知识库向量:
  输入: 文档分块内容
  模型: DashScope text-embedding-v3
  流程: 文档上传 → Tika 解析 → 分块 → 向量化 → ES 入库
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
│  pet-care-gateway  │  pet-care-core  │  pet-care-ai            │
├─────────────────────────────────────────────────────────────────┤
│                        Spring AI 抽象层                          │
│  VectorStore 接口 │ EmbeddingModel │ RetrievalAugmentor         │
├─────────────────────────────────────────────────────────────────┤
│                           同步层                                  │
│  Kafka Producer │ CDC Event │ 异步向量化                        │
├─────────────────────────────────────────────────────────────────┤
│                        存储层 (目标)                              │
│  Elasticsearch 8.x (KNN Plugin)                                  │
│  - knowledge_document 索引                                       │
│  - post 索引                                                    │
│  - label 索引                                                   │
├─────────────────────────────────────────────────────────────────┤
│                        存储层 (源)                                │
│  MySQL │ PgVector(逐步废弃) │ Kafka                              │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 技术选型
| 组件 | 选型                      | 版本                | 说明 |
|------|-------------------------|-------------------|------|
| 搜索引擎 | Elasticsearch           | 8.17              | 支持 KNN 向量检索 |
| 分词器 | IK Analysis             | 8.17              | 中文分词 |
| 同步中间件 | Kafka                   | 3.x               | CDC 事件流 |
| CDC 连接器 | Canal                   | 1.1.7             | MySQL Binlog 捕获 |
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
  4. 部署 Canal

验收:
  - ES 集群健康状态 green
  - KNN 插件可用
  - Kafka 连接正常
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
```yaml
任务:
  1. 全量数据迁移（MySQL → ES）
  2. 增量 CDC 同步（Canal）
  3. 异步向量化服务
  4. 数据一致性校验

验收:
  - 数据条数一致
  - 向量相似度测试通过
```

### 4.5 阶段五：混合检索（1周）
```yaml
任务:
  1. 实现混合检索 DSL 封装
  2. 优化打分权重参数
  3. 性能测试与调优
  4. A/B 测试

验收:
  - 响应时间 < 50ms
  - 推荐效果提升 10%+
```

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
  延迟: < 1s
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