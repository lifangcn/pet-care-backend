# Pet Care System

基于 Spring Boot 的宠物关怀服务平台，提供宠物健康管理、提醒推送、社区互动等功能。

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 3.3.5 |
| Spring Cloud Alibaba | 2023.0.1.2 |
| MyBatis-Flex | 1.11.4 |
| Redis (Redisson) | 3.27.0 |
| Kafka | - |
| MySQL | 9.1.0 |
| Knife4j | 4.5.0 |

## 快速启动

### 1. 依赖服务
- MySQL 8.0+
- Redis 6.0+
- Kafka 2.8+

### 2. 数据库初始化
```bash
mysql -u root -p < scripts/db/init_all.sql
mysql -u root -p < scripts/db/init_default_labels.sql
```

### 3. 配置环境变量
```bash
export JWT_SECRET_KEY=your-secret-key
export OSS_ENDPOINT=your-oss-endpoint
export OSS_ACCESS_KEY_ID=your-access-key-id
export OSS_ACCESS_KEY_SECRET=your-access-key-secret
export OSS_BUCKET_NAME=your-bucket-name
```

### 4. 修改配置
编辑 `modules/pet-care-core/src/main/resources/application.yml`：
- 数据库连接信息
- Redis 连接信息
- Kafka 连接信息

### 5. 编译运行
```bash
mvn clean install
cd modules/pet-care-core
mvn spring-boot:run
```

### 6. 访问
- 服务地址：http://localhost:8080
- API 文档：http://localhost:8080/doc.html

## 模块说明

| 模块 | 说明 |
|------|------|
| pet-care-common | 公共组件 |
| pet-care-core | 核心服务 |
| pet-care-ai | AI 智能助手 |
| pet-care-gateway | 网关服务 |
