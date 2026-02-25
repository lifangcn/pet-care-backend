# 贡献指南

感谢你对 Pet Care System 的关注！欢迎提交 Issue 和 Pull Request。

## 环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Maven | 3.9+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |
| Kafka | 2.8+ (可选) |

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/your-org/petcare.git
cd petcare
```

### 2. 数据库初始化

```bash
mysql -u root -p < scripts/db/init_all.sql
mysql -u root -p < scripts/db/init_default_labels.sql
```

### 3. 配置文件

复制 `application-example.yml`（如有）并修改配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pet_care?useUnicode=true&characterEncoding=utf8
    username: your-username
    password: your-password
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 编译运行

```bash
mvn clean install
cd modules/pet-care-core
mvn spring-boot:run
```

## 开发规范

### 分支命名

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| 功能 | `feature/功能名` | `feature/user-login` |
| 修复 | `fix/问题描述` | `fix/jwt-expire` |
| 重构 | `refactor/模块名` | `refactor/payment` |
| 文档 | `docs/说明` | `docs/api-update` |

### Commit 规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 类型：**

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `refactor` | 重构 |
| `docs` | 文档更新 |
| `test` | 测试相关 |
| `chore` | 构建/工具链 |

**示例：**

```
feat(user): 添加手机号登录功能

- 新增手机号验证码登录接口
- 添加限流防止恶意调用

Closes #123
```

### 代码风格

- 遵循阿里巴巴 Java 开发规范
- 使用 Hutool 工具类，避免重复造轮子
- 方法添加注释：描述、创建时间、作者
```java
/**
 * 用户注册
 *
 * @param request 注册请求
 * @return 用户ID
 * @author Michael Li
 * @since 2025-01-15
 */
```

## 提交 PR

1. **Fork** 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 **Pull Request**

### PR 检查清单

- [ ] 代码通过编译 (`mvn clean install`)
- [ ] 添加必要的注释
- [ ] 更新相关文档
- [ ] Commit 信息符合规范
- [ ] 每个PR只做一件事，避免大而全的修改

## 报告问题

提交 Issue 时请提供：

- 环境信息（JDK 版本、操作系统）
- 复现步骤
- 期望行为 vs 实际行为
- 相关日志或截图

## 行为准则

- 尊重不同观点
- 专注于建设性讨论
- 对社区友好包容
