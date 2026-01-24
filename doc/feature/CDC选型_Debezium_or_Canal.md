资源开销对比

| 组件     | Canal      | Debezium (Kafka Connect)           |
|----------|------------|------------------------------------|
| 最小内存 | 512MB      | 2GB+ (Kafka + Connect + Zookeeper) |
| 部署单元 | 单进程     | Kafka 集群 + Connect 集群          |
| 依赖     | 仅 JDK     | Kafka + Zookeeper + Connect        |
| 磁盘     | 100MB 起步 | 10GB+ (Kafka 日志保留)             |

结论：轻量级服务器（4C8G 以下） Canal 是唯一选择。

  ---
部署复杂度对比

Canal 部署

# 1. 下载解压
wget https://github.com/alibaba/canal/releases/download/canal-1.1.7/canal.deployer-1.1.7.tar.gz
tar -zxvf canal.deployer-1.1.7.tar.gz

# 2. 修改配置
vi conf/example/instance.properties
# 只需修改 3 项
canal.instance.master.address=127.0.0.1:3306
canal.instance.dbUsername=canal
canal.instance.dbPassword=canal

# 3. 启动
sh bin/startup.sh

# 完成

部署时间：5 分钟

  ---
Debezium 部署

# 1. 部署 Zookeeper（必选）
wget kafka_2.13-3.6.0.tgz
配置 zookeeper.properties
启动: bin/zookeeper-server-start.sh

# 2. 部署 Kafka
配置 server.properties
启动: bin/kafka-server-start.sh

# 3. 部署 Kafka Connect
配置 connect-standalone.properties
配置 MySQL connector JSON

# 4. 安装 Debezium Connector
wget debezium-connector-mysql-2.5.2-plugin.tar.gz
放入 plugin.path

# 5. 注册 connector
curl -X POST http://localhost:8083/connectors \
-d @mysql-connector.json

# 完成

部署时间：30-60 分钟（含故障排查）

  ---
基于你项目的建议

场景矩阵

| 场景                  | 推荐方案 | 理由                   |
  |-----------------------|----------|------------------------|
| 开发/测试环境         | Canal    | 单机部署，快速验证     |
| 生产环境 + 无 Kafka   | Canal    | 避免引入 Kafka 集群    |
| 生产环境 + 已有 Kafka | Debezium | 复用现有基建，统一管理 |
| 轻量服务器（4C8G）    | Canal    | 资源占用低             |
| 多数据源同步          | Debezium | 统一 CDC 平台          |

针对你的项目

现状：
- 已有 Kafka（用于提醒系统）
- 规划迁移到 Elasticsearch

建议：

方案A：Canal (推荐，快速落地)
┌─────────────┐     ┌──────────┐     ┌──────────────┐
│   MySQL     │────>│  Canal   │────>│   Kafka      │
│ (主库)      │     │ (单进程) │     │ (已部署)     │
└─────────────┘     └──────────┘     └──────────────┘
│
▼
┌──────────────┐
│  ES Consumer │
└──────────────┘

优点：
- 部署简单，5 分钟搞定
- 资源占用低
- 可直推 Kafka，复用现有基建

缺点：
- 需单独运维 Canal 进程

方案B：Debezium (长期规范)
┌─────────────┐     ┌─────────────┐     ┌──────────────┐
│   MySQL     │────>│  Debezium   │────>│   Kafka      │
│ (主库)      │     │(Kafka Connect│     │ (已部署)     │
└─────────────┘     └─────────────┘     └──────────────┘
│
▼
┌──────────────┐
│  ES Consumer │
└──────────────┘

优点：
- Kafka Connect 统一管理
- Offset 自动管理
- 监控完善

缺点：
- 需部署 Kafka Connect Worker（增加资源）
- 配置复杂

最终建议

阶段一（验证期）：用 Canal
- 快速验证 CDC 到 ES 的同步方案
- 验证数据一致性

阶段二（生产期）：
- 如果是轻量服务器 → 继续 Canal
- 如果资源充足 → 迁移到 Debezium（复用 Kafka Connect）