# 企业级改造指南

> 当前项目成熟度：Level 4/5（中高级）
> 创建时间：2025-02-27
> 作者：Michael Li

---

## 一、当前状态评估

### 已具备的企业级特性
- 微服务架构 + DDD 领域划分
- 多层缓存（Redis + Caffeine）
- 分布式锁（Redisson）
- 定时任务调度（XXL-JOB）
- 容器化部署（Docker）
- 指标暴露（Prometheus）

### 核心差距
| 维度 | 状态 | 优先级 |
|------|------|--------|
| CI/CD 流水线 | ❌ 缺失 | P0 |
| 配置中心化 | ⚠️ 本地文件 | P0 |
| 测试体系 | ❌ 缺失 | P1 |
| 可观测性 | ⚠️ 仅指标 | P1 |
| 服务治理 | ⚠️ 基础 | P2 |

---

## 二、改造方案（按优先级）

### P0 - CI/CD 流水线

**文件位置**：`.github/workflows/`

#### 1. CI 流水线（ci.yml）

```yaml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build with Maven
        run: mvn clean compile

      - name: Run Tests
        run: mvn test

      - name: Package
        run: mvn package -DskipTests

      - name: Build Docker Images
        run: |
          docker build -t pet-care-gateway:${{ github.sha }} ./modules/pet-care-gateway
          docker build -t pet-care-core:${{ github.sha }} ./modules/pet-care-core
          docker build -t pet-care-ai:${{ github.sha }} ./modules/pet-care-ai
```

#### 2. 发布流水线（release.yml）

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build & Package
        run: mvn clean package -DskipTests

      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Push Images
        run: |
          VERSION=${GITHUB_REF#refs/tags/v}
          docker build -t yourregistry/pet-care-gateway:$VERSION ./modules/pet-care-gateway
          docker build -t yourregistry/pet-care-core:$VERSION ./modules/pet-care-core
          docker build -t yourregistry/pet-care-ai:$VERSION ./modules/pet-care-ai
          docker push yourregistry/pet-care-gateway:$VERSION
          docker push yourregistry/pet-care-core:$VERSION
          docker push yourregistry/pet-care-ai:$VERSION
```

**GitHub Secrets 配置**：
- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`

---

### P0 - 配置中心化（Nacos Config）

#### 迁移步骤

**1. 确认 Nacos 服务**

确保 Nacos 已部署（建议使用 Docker）：

```bash
docker run -d \
  --name nacos \
  -e MODE=standalone \
  -p 8848:8848 \
  -p 9848:9848 \
  nacos/nacos-server:v2.3.0
```

**2. 修改 bootstrap 配置**

在各模块的 `bootstrap-docker.yml` 中添加：

```yaml
spring:
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_ADDR:nacos-host:8848}
        namespace: ${NACOS_NAMESPACE:}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        file-extension: yml
        shared-configs:
          - dataId: common.yaml
            group: DEFAULT_GROUP
            refresh: true
```

**3. 迁移配置到 Nacos**

| 配置项 | Data ID | 说明 |
|--------|---------|------|
| 公共配置 | `common.yaml` | Redis、Kafka 等基础配置 |
| 网关配置 | `gateway-service.yaml` | 路由、限流配置 |
| 核心服务 | `core-service.yaml` | 业务配置 |
| AI 服务 | `ai-service.yaml` | AI、知识库配置 |

**4. 使用 @RefreshScope 实现热更新**

```java
@RefreshScope
@Configuration
public class SomeConfig {
    @Value("${some.dynamic.config}")
    private String config;
}
```

**5. 敏感信息加密**

使用 Nacos 的 `cipher-` 前缀加密敏感配置：

```yaml
# Nacos 配置
datasource:
  password: cipher(AQCFHKg6WqHv...)
```

---

### P1 - 可观测性体系

#### 1. 日志聚合（Loki + Grafana）

**docker-compose.yml 添加**：

```yaml
services:
  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"

  promtail:
    image: grafana/promtail:latest
    volumes:
      - /var/log:/var/log:ro
      - ./promtail-config.yml:/etc/promtail/config.yml

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

**Logback 配置（添加 JSON 输出）**：

```xml
<appender name="LOKI" class="com.github.loki4j.logback.Loki4jAppender">
    <httpUrl>http://loki:3100/loki/api/v1/push</httpUrl>
    <format>
        <label>
            <pattern>app=pet-care,level=%level,service=${SERVICE_NAME}</pattern>
        </label>
        <message>
            <json/>
        </message>
    </format>
</appender>
```

#### 2. 监控仪表盘（Grafana）

**导入预置面板**：
- Spring Boot Dashboard (ID: 4701)
- JVM Dashboard (ID: 4701)
- Redis Dashboard (ID: 11835)

#### 3. 链路追踪（Skywalking）

**部署 Skywalking**：

```bash
docker run -d --name skywalking-oap \
  -p 11800:11800 -p 12800:12800 \
  apache/skywalking-oap-server:latest

docker run -d --name skywalking-ui \
  -p 8080:8080 \
  -e SW_OAP_ADDRESS=http://skywalking-oap:12800 \
  apache/skywalking-ui:latest
```

**Java Agent 配置**：

```bash
java -javaagent:/path/to/skywalking-agent.jar \
     -Dskywalking.agent.service_name=pet-care-core \
     -Dskywalking.collector.backend_service=skywalking-oap:11800 \
     -jar pet-care-core.jar
```

---

### P1 - 测试体系

#### 1. 依赖配置（pom.xml）

```xml
<dependencies>
    <!-- 单元测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- 集成测试 - Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### 2. 单元测试示例

```java
@SpringBootTest
class UserServiceTest {

    @MockBean
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Test
    void shouldCreateUser() {
        // Given
        User user = new User();
        user.setUsername("test");
        when(userMapper.insert(any())).thenReturn(1);

        // When
        Long id = userService.createUser(user);

        // Then
        assertThat(id).isNotNull();
    }
}
```

#### 3. 集成测试示例

```java
@Testcontainers
@SpringBootTest
class UserServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Test
    void shouldPersistUser() {
        // 测试真实数据库操作
    }
}
```

---

### P2 - 服务治理

#### 1. Sentinel 限流熔断

**依赖配置**：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

**配置（application.yml）**：

```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: sentinel-dashboard:8080
      datasource:
        flow:
          nacos:
            server-addr: nacos-host:8848
            dataId: ${spring.application.name}-sentinel-flow
            rule-type: flow
```

**限流规则（Nacos 配置）**：

```yaml
[
  {
    "resource": "/api/v1/users",
    "grade": 1,
    "count": 100,
    "strategy": 0
  }
]
```

#### 2. 健康检查增强

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

---

## 三、实施检查清单

### 阶段一：基础设施（1-2周）
- [ ] 部署 Nacos 配置中心
- [ ] 创建 GitHub Actions CI 流水线
- [ ] 创建 GitHub Actions Release 流水线
- [ ] 配置 Docker Registry

### 阶段二：可观测性（1周）
- [ ] 部署 Loki + Promtail
- [ ] 部署 Grafana 并导入仪表盘
- [ ] 配置应用日志输出为 JSON 格式
- [ ] 部署 Skywalking（可选）

### 阶段三：质量保障（持续）
- [ ] 编写核心业务单元测试
- [ ] 编写集成测试用例
- [ ] 配置测试覆盖率检查

### 阶段四：服务治理（按需）
- [ ] 接入 Sentinel
- [ ] 配置限流熔断规则
- [ ] 完善健康检查端点

---

## 四、验证标准

### CI/CD
- [ ] 推送代码自动触发构建
- [ ] 测试失败阻止合并
- [ ] 发布标签自动构建镜像

### 配置中心
- [ ] 敏感信息不在代码仓库
- [ ] 修改配置无需重启服务

### 可观测性
- [ ] 日志可查询
- [ ] 指标有仪表盘
- [ ] 调用链可追踪

### 测试
- [ ] 核心业务有单元测试
- [ ] 覆盖率 > 60%

---

## 五、参考资源

- [Nacos 配置中心文档](https://nacos.io/docs/latest/guide/admin/configuration-management/)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [Skywalking 文档](https://skywalking.apache.org/docs/)
- [Testcontainers 文档](https://java.testcontainers.org/)
