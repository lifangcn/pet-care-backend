# Pet Care System

[![Java Version](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

基于 Spring Boot 的宠物关怀服务平台，提供宠物健康管理、提醒推送、社区互动、积分奖励等功能。

## 功能特性

- **用户认证**：JWT 令牌认证，支持手机号登录
- **宠物管理**：宠物档案、健康记录、疫苗接种追踪
- **提醒服务**：疫苗提醒、体检提醒、定时任务调度
- **社区互动**：动态发布、点赞评论、标签分类
- **积分系统**：签到奖励、积分兑换、积分劵管理
- **AI 助手**：智能问答、宠物健康知识库

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

### 6. 在线演示

- 服务地址：https://michaelli.site
- API 文档：https://michaelli.site/doc.html

### 7. 本地访问

- 服务地址：http://localhost:8080
- API 文档：http://localhost:8080/doc.html

## 模块说明

| 模块 | 说明 |
|------|------|
| pet-care-common | 公共组件、工具类 |
| pet-care-core | 核心服务（用户、宠物、提醒、社区、积分） |
| pet-care-ai | AI 智能助手、知识库 |

## 项目结构

```
petcare/
├── doc/                    # 文档目录
│   ├── feature/           # 功能设计文档
│   ├── fix/               # 问题修复记录
│   ├── performance/       # 性能测试报告
│   └── refactor/          # 重构记录
├── modules/
│   ├── pet-care-common/   # 公共模块
│   ├── pet-care-core/     # 核心服务
│   └── pet-care-ai/       # AI 服务
├── scripts/               # 脚本（数据库初始化等）
├── LICENSE                # MIT 许可证
├── CONTRIBUTING.md        # 贡献指南
└── README.md
```

## 贡献

欢迎提交 Issue 和 Pull Request，详见 [贡献指南](CONTRIBUTING.md)。

## 许可证

本项目基于 [MIT 许可证](LICENSE) 开源。
