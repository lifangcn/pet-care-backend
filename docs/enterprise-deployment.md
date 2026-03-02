# PetCare 企业级部署文档

> 创建时间：2026-02-27
> 作者：Michael Li
> 服务器：Ubuntu 22.04 / 16GB RAM / 78GB 可用磁盘
> 更新时间：2026-02-28

---

## 一、架构概览

### 1.1 最终部署架构

```
                                   Internet
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Nginx (Port 80)                          │
│                    反向代理 + 统一入口                            │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌─────────────────┐   ┌─────────────────┐
│    Gitea      │   │   Jenkins       │   │   Grafana       │
│  Port: 3000   │   │   Port: 8083    │   │   Port: 9091    │
│  代码管理      │   │   CI/CD         │   │   监控面板       │
└───────────────┘   └─────────────────┘   └─────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      应用服务层                                   │
│  pet-care-core (8080)      pet-care-ai (8081)                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      基础设施层                                   │
│  MySQL(3306)  Redis(6379)  Nacos(8848)  XXL-JOB(9095)            │
│  ES(9200)    Prometheus(9090)                                    │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 服务端口分配

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 统一入口 |
| Gitea | 3000, 222 | 代码管理 |
| Jenkins | 8083, 50000 | CI/CD |
| Grafana | 3001 | 监控面板 |
| Prometheus | 9090 | 监控数据采集 |
| pet-care-core | 8080 | 核心服务 |
| pet-care-ai | 8081 | AI服务 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nacos | 8848, 9848 | 服务发现 |
| XXL-JOB | 9095 | 定时任务 |
| Elasticsearch | 9200, 9300 | 搜索引擎 |

---

## 二、完整部署流程

### 2.1 前置准备

```bash
# 1. 更新系统
sudo apt update && sudo apt upgrade -y

# 2. 安装基础工具
sudo apt install -y curl wget git vim net-tools

# 3. 安装 Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker

# 4. 安装 Docker Compose
sudo apt install -y docker-compose-plugin

# 5. 安装 Nginx
sudo apt install -y nginx

# 6. 创建目录结构
sudo mkdir -p /opt/petcare/{infra,app,data/{mysql,redis,nacos,xxl-job,gitea,jenkins,logs}}
```

### 2.2 部署基础设施服务

**创建配置文件** `/opt/petcare/infra/docker-compose.infra.yml`：

```yaml
version: '3.8'

services:
  # MySQL 数据库
  petcare-mysql:
    image: mysql:8.0
    container_name: petcare-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: PetCare@2026!Secure
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - /opt/petcare/data/mysql:/var/lib/mysql
      - /opt/petcare/infra/mysql-init:/docker-entrypoint-initdb.d
    command:
      - --default-authentication-plugin=mysql_native_password
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --max_connections=1000
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - petcare-network

  # Redis 缓存
  petcare-redis:
    image: redis:7-alpine
    container_name: petcare-redis
    restart: always
    ports:
      - "6379:6379"
    volumes:
      - /opt/petcare/data/redis:/data
    command: redis-server --appendonly yes
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - petcare-network

  # Nacos 注册中心
  petcare-nacos:
    image: nacos/nacos-server:v2.3.0
    container_name: petcare-nacos
    restart: always
    environment:
      MODE: standalone
      MYSQL_SERVICE_HOST: host.docker.internal
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos_config
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: PetCare@2026!Secure
      SPRING_DATASOURCE_PLATFORM: mysql
      JVM_XMS: 512m
      JVM_XMX: 512m
      TZ: Asia/Shanghai
    ports:
      - "8848:8848"
      - "9848:9848"
    depends_on:
      - petcare-mysql
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - petcare-network

  # XXL-JOB 定时任务
  petcare-xxl-job:
    image: xuxueli/xxl-job-admin:3.2.0
    container_name: petcare-xxl-job
    restart: always
    environment:
      PARAMS: "--spring.datasource.url=jdbc:mysql://host.docker.internal:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai --spring.datasource.username=root --spring.datasource.password=PetCare@2026!Secure"
    ports:
      - "9095:9095"
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - petcare-network

networks:
  petcare-network:
    name: petcare-network
    driver: bridge
```

**创建数据库初始化脚本** `/opt/petcare/infra/mysql-init/01-init-databases.sql`：

```sql
-- 创建业务数据库
CREATE DATABASE IF NOT EXISTS pet_care_core DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS pet_care_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建基础设施数据库
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS gitea DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**启动基础设施**：

```bash
cd /opt/petcare/infra
docker compose -f docker-compose.infra.yml up -d

# 等待 MySQL 启动并初始化
sleep 30

# 检查服务状态
docker compose -f docker-compose.infra.yml ps
```

### 2.3 部署 CI/CD 平台

**创建配置文件** `/opt/petcare/infra/docker-compose.cicd.yml`：

```yaml
services:
  # Gitea 代码仓库
  gitea:
    image: gitea/gitea:1.21
    container_name: petcare-gitea
    restart: always
    environment:
      - USER_UID=1000
      - USER_GID=1000
      - GITEA__database__DB_TYPE=mysql
      - GITEA__database__HOST=host.docker.internal:3306
      - GITEA__database__NAME=gitea
      - GITEA__database__USER=root
      - GITEA__database__PASSWD=${MYSQL_ROOT_PASSWORD}
    ports:
      - "3000:3000"
      - "222:22"
    volumes:
      - ./data/gitea:/data
      - /etc/timezone:/etc/timezone:ro
      - /etc/localtime:/etc/localtime:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - petcare-network

  # Jenkins 持续集成
  jenkins:
    image: petcare-jenkins-with-maven:latest
    container_name: petcare-jenkins
    restart: always
    group_add:
      - "999"
    volumes:
      - ./data/jenkins:/var/jenkins_home
      - /opt/petcare/infra:/opt/petcare/infra
      - /opt/petcare/app:/opt/petcare/app
      - /var/run/docker.sock:/var/run/docker.sock
      - /usr/bin/docker:/usr/bin/docker:ro
      - /usr/libexec/docker/cli-plugins:/usr/libexec/docker/cli-plugins:ro
    environment:
      - JAVA_OPTS=-Xms512m -Xmx1024m
      - JENKINS_OPTS=
    extra_hosts:
      - "host.docker.internal:host-gateway"
    networks:
      - petcare-network

networks:
  petcare-network:
    name: petcare-network
    external: true
```

**启动 CI/CD**：

```bash
cd /opt/petcare/infra
docker compose -f docker-compose.cicd.yml up -d

# 等待 Jenkins 启动
sleep 30

# 获取 Jenkins 初始密码
docker logs petcare-jenkins 2>&1 | grep -A 5 "Jenkins initial Admin"
```

**配置 Jenkins（首次访问）**：

1. 访问 http://192.168.31.100:8083
2. 解锁 Jenkins（使用初始密码）
3. 安装推荐插件
4. 创建管理员用户
5. 安装 Maven 插件（如果需要）

### 2.4 配置 Gitea 仓库

**在 Gitea 中创建仓库**：

1. 访问 http://192.168.31.100:3000
2. 首次登录设置管理员账号
3. 创建仓库 `pet-care-backend`
4. 使用 **迁移外部仓库** 功能：
   - 仓库地址：`https://github.com/lifangcn/pet-care-backend.git`
   - 镜像方向：从外部仓库推送到 Gitea

**配置 Webhook（可选，实现自动触发）**：

在 Gitea 仓库设置 -> Webhooks -> 添加 webhook：
- URL: `http://192.168.31.100:8083/gitea-webhook/`
- 触发事件：推送事件

### 2.5 配置 Jenkins Pipeline

**创建 Jenkins Pipeline 任务**：

1. 访问 http://192.168.31.100:8083
2. 新建任务 -> Pipeline
3. 任务名称：`petcare-deploy`
4. 配置 Pipeline：
   - Definition：Pipeline script from SCM
   - SCM：Git
   - Repository URL：`http://192.168.31.100:3000/michaelli423/pet-care-backend.git`
   - Script Path: `Jenkinsfile`

**创建 Jenkinsfile**（已存在于项目根目录）：

```groovy
pipeline {
    agent any

    stages {
        stage('Deploy') {
            steps {
                sh 'docker compose -f /opt/petcare/infra/docker-compose.app.yml up -d'
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    echo "等待服务启动..."
                    sleep 60
                    curl -f http://192.168.31.100:8080/actuator/health || exit 1
                    curl -f http://192.168.31.100:8081/actuator/health || exit 1
                    echo "✅ 健康检查通过！"
                '''
            }
        }
    }

    post {
        success {
            echo '✅ 部署成功！'
            echo "Core API: http://192.168.31.100:8080"
            echo "AI API:  http://192.168.31.100:8081"
        }
        failure {
            echo '❌ 部署失败，请检查日志'
        }
    }
}
```

### 2.6 部署应用服务

**创建配置文件** `/opt/petcare/infra/docker-compose.app.yml`：

```yaml
services:
  pet-care-core:
    build:
      context: /opt/petcare/app
      dockerfile: modules/pet-care-core/Dockerfile
    image: infra-pet-care-core
    container_name: petcare-core
    restart: always
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MYSQL_HOST=petcare-mysql
      - MYSQL_USER=root
      - MYSQL_PASSWORD=PetCare@2026!Secure
      - REDIS_HOST=petcare-redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=!QAZ2wsx
      - NACOS_HOST=petcare-nacos
      - ES_HOST_HOST=host.docker.internal
      - OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
      - OSS_ACCESS_KEY_ID={your_access_key_id}
      - OSS_ACCESS_KEY_SECRET={your_access_key_secret}
      - OSS_BUCKET_NAME=lifang-pet-care
      - ALIYUN_OSS_ENABLED=true
      - JWT_SECRET_KEY=M9Ttt6uwxsaq6PMmFBNQekNWjaFXy+WJSQVQB5IOzdQ=
    networks:
      - petcare-network
    extra_hosts:
      - "host.docker.internal:host-gateway"

  pet-care-ai:
    build:
      context: /opt/petcare/app
      dockerfile: modules/pet-care-ai/Dockerfile
    image: infra-pet-care-ai
    container_name: petcare-ai
    restart: always
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - MYSQL_HOST=petcare-mysql
      - MYSQL_USER=root
      - MYSQL_PASSWORD=PetCare@2026!Secure
      - REDIS_HOST=petcare-redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=!QAZ2wsx
      - NACOS_HOST=petcare-nacos
      - ES_HOST_HOST=host.docker.internal
      - ZHIPU_AI_API_KEY={your_api_key}
      - OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
      - OSS_ACCESS_KEY_ID={your_access_key_id}
      - OSS_ACCESS_KEY_SECRET={your_access_key_secret}
      - OSS_BUCKET_NAME=lifang-pet-care
      - ALIYUN_OSS_ENABLED=true
      - JWT_SECRET_KEY=M9Ttt6uwxsaq6PMmFBNQekNWjaFXy+WJSQVQB5IOzdQ=
    networks:
      - petcare-network
    extra_hosts:
      - "host.docker.internal:host-gateway"

networks:
  petcare-network:
    name: petcare-network
    external: true
```

**创建 Dockerfile** `/opt/petcare/app/modules/pet-care-core/Dockerfile`：

```dockerfile
FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM amazoncorretto:21
WORKDIR /app
COPY --from=builder /app/modules/pet-care-core/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**创建 Dockerfile** `/opt/petcare/app/modules/pet-care-ai/Dockerfile`：

```dockerfile
FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM amazoncorretto:21
WORKDIR /app
COPY --from=builder /app/modules/pet-care-ai/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**首次部署（构建镜像）**：

```bash
cd /opt/petcare/infra
docker compose -f docker-compose.app.yml up -d --build
```

**后续更新（只需重启容器）**：

```bash
cd /opt/petcare/infra
docker compose -f docker-compose.app.yml up -d
```

**如果代码有重大改动需要重新构建镜像**：

```bash
cd /opt/petcare/infra
docker compose -f docker-compose.app.yml up -d --build
```

---

## 三、Jenkins CI/CD 踩坑记录

### 3.1 Jenkins 容器内 Maven 缺失

**问题**：
```
mvn: not found
```

**原因**：Jenkins 官方镜像不包含 Maven

**解决**：在 Jenkins 容器内安装 Maven 并保存为新镜像

```bash
# 在 Jenkins 容器内安装 Maven
docker exec -u 0 petcare-jenkins bash -c 'apt-get update && apt-get install -y maven'

# 提交为新镜像
docker commit petcare-jenkins petcare-jenkins-with-maven:latest

# 更新 docker-compose.cicd.yml 使用新镜像
image: petcare-jenkins-with-maven:latest
```

### 3.2 Jenkins 容器无 Docker 访问权限

**问题**：
```
permission denied while trying to connect to the docker API socket
```

**解决**：将 Jenkins 用户添加到 docker 组（GID 999）

```yaml
# docker-compose.cicd.yml
jenkins:
  group_add:
    - "999"
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock
```

### 3.3 Jenkins 容器无法访问主机目录

**问题**：
```
AccessDeniedException: /opt/petcare/infra@tmp
```

**原因**：
1. Jenkins `dir()` 步骤尝试在目标目录创建临时文件
2. Jenkins 容器用户（UID 1000）无权限写入 `/opt/petcare/infra`

**解决**：
1. 不使用 `dir()` 切换目录
2. 直接使用绝对路径执行命令
3. 挂载必要的目录到容器

### 3.4 Docker 构建上下文路径问题

**问题**：
```
unable to prepare context: path "/opt/petcare/app" not found
```

**原因**：Jenkins 容器内没有挂载 `/opt/petcare/app`

**解决**：挂载应用目录到 Jenkins 容器

```yaml
# docker-compose.cicd.yml
volumes:
  - /opt/petcare/app:/opt/petcare/app
```

### 3.5 Maven 构建缺少父 POM

**问题**：
```
Non-resolvable parent POM for pvt.mktech.petcare:pet-care-ai
Could not find artifact pvt.mktech.petcare:pet-care-system:pom:1.0-SNAPSHOT
```

**原因**：
1. Dockerfile 只复制了单个模块的 pom.xml
2. 根 pom.xml（父 POM）不在构建上下文中

**解决**：使用完整项目作为构建上下文

```dockerfile
# /opt/petcare/app/modules/pet-care-core/Dockerfile
FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /app
COPY . .  # 复制整个项目
RUN mvn clean package -DskipTests

FROM amazoncorretto:21
WORKDIR /app
COPY --from=builder /app/modules/pet-care-core/target/*.jar app.jar
```

### 3.6 健康检查 localhost 无法访问

**问题**：
```
curl: (22) The requested URL returned error: 403
```

**原因**：Jenkins 容器内 `localhost` 指向容器自己，不是主机

**解决**：使用主机 IP `192.168.31.100`

```groovy
// Jenkinsfile
stage('Health Check') {
    steps {
        sh '''
            curl -f http://192.168.31.100:8080/actuator/health || exit 1
            curl -f http://192.168.31.100:8081/actuator/health || exit 1
        '''
    }
}
```

### 3.7 Jenkins workspace 使用旧代码

**问题**：Jenkins 拉取代码后使用的是旧版本

**原因**：Jenkins workspace 缓存

**解决**：每次构建前清理 workspace

```bash
# 手动清理
rm -rf /opt/petcare/infra/data/jenkins/workspace/petcare-deploy/*
```

或在 Jenkins 中配置 "Clean before checkout"

### 3.8 Jenkins Pipeline 使用 rsync 失败

**问题**：
```
rsync: not found
```

**原因**：Jenkins 容器没有安装 rsync

**解决**：使用 `cp` 命令替代

```groovy
// 不推荐
rsync -av --delete $WORKSPACE/modules/... /opt/petcare/app/modules/...

// 推荐
rm -rf /opt/petcare/app/modules/.../*
cp -r $WORKSPACE/modules/... /opt/petcare/app/modules/...
```

**最终方案**：不需要复制文件，直接在 `/opt/petcare/app` 构建

### 3.9 Kafka 服务缺失

**问题**：
```
Connection to node -1 (localhost/127.0.0.1:9092) could not be established
Bootstrap broker localhost:9092 disconnected
```

**原因**：应用依赖 Kafka 但未部署相关服务

**解决**：添加 Zookeeper 和 Kafka 服务到 `docker-compose.infra.yml`

```yaml
# docker-compose.infra.yml
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  container_name: petcare-zookeeper
  restart: always
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
    ZOOKEEPER_TICK_TIME: 2000
  ports:
    - "2181:2181"
  networks:
    - petcare-network

kafka:
  image: confluentinc/cp-kafka:7.5.0
  container_name: petcare-kafka
  restart: always
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
  ports:
    - "9094:9092"  # 注意：9092/9093/9095 被其他服务占用
  depends_on:
    - zookeeper
  networks:
    - petcare-network
```

在 `docker-compose.app.yml` 添加环境变量：
```yaml
environment:
  - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

### 3.10 数据库名称配置错误

**问题**：
```
Table 'petcare.tb_user' doesn't exist
```

**原因**：
1. `application-docker.yml` 中硬编码了数据库名 `petcare`
2. JAR 包内包含旧的配置文件

**解决**：

1. 修改配置文件使用环境变量：
```yaml
# application-docker.yml
url: jdbc:mysql://${MYSQL_HOST:mysql}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:pet_care_core}?...
```

2. 在 docker-compose 中设置环境变量：
```yaml
# docker-compose.app.yml
environment:
  - MYSQL_DATABASE=pet_care_core
```

3. 重新构建 JAR 包和镜像：
```bash
cd /opt/petcare/app
mvn clean package -DskipTests
cd /opt/petcare/infra
docker compose -f docker-compose.app.yml build
docker compose -f docker-compose.app.yml up -d
```

### 3.11 前端静态资源 API 地址错误

**问题**：
```
POST http://petcare.com/api/auth/code net::ERR_CONNECTION_REFUSED
```

**原因**：前端构建时 API 地址配置为 `petcare.com`

**解决**：修改 `.env.development` 并重新构建

```bash
# 修改前端环境变量
cd /Users/michael/VueProjects/pet-care-vue
echo "VITE_API_BASE_URL=/api" > .env.development

# 重新构建
npm run build

# 上传到服务器
ssh root@192.168.31.100 "rm -rf /opt/petcare/frontend/html/*"
scp -r dist/* root@192.168.31.100:/opt/petcare/frontend/html/
```

### 3.12 Dockerfile 构建路径错误

**问题**：
```
failed to calculate checksum: "/target/pet-care-ai-1.0-SNAPSHOT.jar": not found
```

**原因**：Dockerfile 中的路径相对于 build context 不正确

**解决**：修改 Dockerfile 中的相对路径

```dockerfile
# 错误（build context 是 /opt/petcare/app）
COPY target/pet-care-core-1.0-SNAPSHOT.jar app.jar

# 正确
COPY modules/pet-care-core/target/pet-care-core-1.0-SNAPSHOT.jar app.jar
```

**build context 配置**：
```yaml
# docker-compose.app.yml
pet-care-core:
  build:
    context: /opt/petcare/app              # 构建上下文根目录
    dockerfile: modules/pet-care-core/Dockerfile
```

---

## 四、完整 CI/CD 工作流程

### 4.1 日常开发部署流程

```bash
# 1. 本地开发并提交
git add .
git commit -m "feat: xxx"
git push

# 2. Gitea 自动同步（约10分钟延迟）

# 3. 手动触发 Jenkins 构建
# 访问 http://192.168.31.100:8083/job/petcare-deploy
# 点击 "Build Now"

# 4. Jenkins 自动执行：
# - 从 Gitea 拉取最新代码
# - 重启容器（不重新构建镜像）
# - 健康检查
```

### 4.2 代码重大改动流程

```bash
# 1. 本地提交并推送
git push

# 2. 等待 Gitea 同步

# 3. 在服务器上重新构建镜像
ssh root@192.168.31.100
cd /opt/petcare/infra
docker compose -f docker-compose.app.yml up -d --build

# 4. 或者在 Jenkinsfile 中添加 --build 参数（构建会较慢）
```

### 4.3 自动化改进方案（可选）

**方案1：Gitea Webhook**

在 Gitea 配置 Webhook 自动触发 Jenkins 构建：
- URL: `http://192.168.31.100:8083/gitea-webhook/`
- 需要公网 IP 或内网穿透

**方案2：Jenkins 定时轮询**

在 Jenkins 任务配置中添加 SCM 轮询：
- 轮询间隔：`H/10 * * * *`（每10分钟）
- 自动检测 Gitea 更新并触发构建

**方案3：手动同步 + 构建**

```bash
# 手动同步 Gitea
cd /tmp
rm -rf petcare-sync
git clone http://192.168.31.100:3000/michaelli423/pet-care-backend.git
cd petcare-sync
git pull
git push

# 触发 Jenkins 构建
curl -X POST -u admin:password http://192.168.31.100:8083/job/petcare-deploy/build
```

---

## 五、访问地址与默认密码

| 服务 | 访问地址 | 默认账号 | 默认密码 |
|------|---------|---------|---------|
| 前端页面 | http://192.168.31.100 | - | - |
| Gitea | http://192.168.31.100:3000 | 首次访问设置 | - |
| Jenkins | http://192.168.31.100:8083 | admin | (查看日志获取) |
| Core API | http://192.168.31.100:8080 | - | - |
| AI API | http://192.168.31.100:8081 | - | - |
| Knife4j (Core) | http://192.168.31.100:8080/doc.html | - | - |
| Knife4j (AI) | http://192.168.31.100:8081/doc.html | - | - |

---

## 六、配置文件路径与作用说明

### 6.1 目录结构

```
/opt/petcare/
├── app/                          # 应用源代码（从 GitHub 克隆）
│   ├── modules/
│   │   ├── pet-care-core/
│   │   │   └── Dockerfile        # Core 服务构建配置
│   │   ├── pet-care-ai/
│   │   │   └── Dockerfile        # AI 服务构建配置
│   │   └── pet-care-common/
│   ├── pom.xml                   # 父 POM（Maven 多模块构建）
│   └── Jenkinsfile               # Jenkins Pipeline 定义
├── infra/                        # 基础设施配置
│   ├── docker-compose.infra.yml  # 基础设施服务编排
│   ├── docker-compose.cicd.yml   # CI/CD 服务编排
│   ├── docker-compose.app.yml    # 应用服务编排
│   ├── docker-compose.frontend.yml # 前端 Nginx 服务编排
│   └── data/                     # 数据持久化目录
│       ├── mysql/                # MySQL 数据
│       ├── redis/                # Redis 数据
│       ├── nacos/                # Nacos 配置
│       ├── gitea/                # Gitea 数据
│       └── jenkins/              # Jenkins 工作空间
├── frontend/                     # 前端静态文件
│   ├── html/                     # Nginx 静态文件目录
│   ├── config/
│   │   └── nginx.conf            # Nginx 配置文件
│   └── data/logs/                # Nginx 日志
├── deploy.sh                     # 备用手动部署脚本
└── services/                     # 额外服务配置
```

### 6.2 核心配置文件

| 路径 | 作用 | 关键配置 |
|------|------|----------|
| `/opt/petcare/infra/docker-compose.infra.yml` | 基础设施服务编排 | MySQL, Redis, Nacos, XXL-JOB, ES, Kafka |
| `/opt/petcare/infra/docker-compose.cicd.yml` | CI/CD 服务编排 | Jenkins, Gitea |
| `/opt/petcare/infra/docker-compose.app.yml` | 应用服务编排 | pet-care-core, pet-care-ai |
| `/opt/petcare/infra/docker-compose.frontend.yml` | 前端 Nginx 服务编排 | Nginx 反向代理 + 静态文件 |
| `/opt/petcare/infra/docker-compose.monitor.yml` | 监控服务编排 | Prometheus, Grafana, cAdvisor, Loki |
| `/opt/petcare/app/modules/pet-care-core/Dockerfile` | Core 服务镜像构建 | 单阶段构建，复制 JAR 包 |
| `/opt/petcare/app/modules/pet-care-ai/Dockerfile` | AI 服务镜像构建 | 单阶段构建，复制 JAR 包 |
| `/opt/petcare/frontend/config/nginx.conf` | Nginx 配置 | 反向代理，API 路由，静态文件服务 |
| `/opt/petcare/app/modules/*/src/main/resources/application-docker.yml` | 应用 Docker 配置 | 数据源、Redis、Kafka 等 |
| `Jenkinsfile`（代码库根目录） | Jenkins Pipeline 定义 | 构建部署流程，通过 Gitea 同步 |

### 6.3 Jenkins 容器挂载点

```yaml
volumes:
  - /opt/petcare/infra:/opt/petcare/infra      # 访问 docker-compose 文件
  - /opt/petcare/app:/opt/petcare/app          # 访问应用源码
  - /var/run/docker.sock:/var/run/docker.sock  # Docker socket 访问
  - /usr/bin/docker:/usr/bin/docker:ro         # Docker CLI
```

### 6.4 配置关键点

**docker-compose.cicd.yml**：
- Jenkins 需挂载 `/opt/petcare/infra` 和 `/opt/petcare/app`
- 需挂载 Docker socket 和 docker CLI
- 用户需加入 docker 组（GID 999）

**docker-compose.app.yml**：
- build context 是 `/opt/petcare/app`（完整项目）
- dockerfile 路径：`modules/pet-care-core/Dockerfile`
- 镜像名称：`infra-pet-care-core` 和 `infra-pet-care-ai`
- 环境变量：`MYSQL_DATABASE`、`KAFKA_BOOTSTRAP_SERVERS` 等

**docker-compose.frontend.yml**：
- Nginx 挂载静态文件目录 `/opt/petcare/frontend/html`
- Nginx 配置文件：`/opt/petcare/frontend/config/nginx.conf`
- upstream 配置使用容器名：`petcare-core:8080`、`petcare-ai:8081`

**Dockerfile**：
- 路径需相对于 build context：`modules/pet-care-core/target/*.jar`
- 使用 `ENTRYPOINT` 而非 `CMD`（profile 由环境变量控制）

**Jenkinsfile**：
- 使用主机 IP `192.168.31.100` 而非 `localhost`
- 健康检查等待：60 秒
- 流程：重启容器（不重新构建镜像）

**application-docker.yml**：
- 数据源 URL 必须使用环境变量：`${MYSQL_DATABASE:pet_care_core}`
- Kafka 地址：`${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`
- Redis 地址：`${REDIS_HOST:redis}`

**前端构建配置**：
- `.env.development`: `VITE_API_BASE_URL=/api`（相对路径）
- 构建命令：`npm run build`
- 静态文件上传：`scp -r dist/* root@server:/opt/petcare/frontend/html/`

---

## 七、服务管理命令

### 7.1 查看服务状态

```bash
cd /opt/petcare/infra

# 查看基础设施
docker compose -f docker-compose.infra.yml ps

# 查看 CI/CD
docker compose -f docker-compose.cicd.yml ps

# 查看应用
docker compose -f docker-compose.app.yml ps
```

### 7.2 查看日志

```bash
# 应用服务日志
docker logs -f petcare-core --tail 100
docker logs -f petcare-ai --tail 100

# Jenkins 日志
docker logs petcare-jenkins --tail 100

# 查看最近构建日志
ls -la /opt/petcare/infra/data/jenkins/jobs/petcare-deploy/builds/
```

### 7.3 重新构建镜像

```bash
cd /opt/petcare/infra

# 强制重新构建
docker compose -f docker-compose.app.yml build --no-cache

# 构建并启动
docker compose -f docker-compose.app.yml up -d
```

---

## 八、问题排查指南

### 8.1 Jenkins 构建失败

1. 检查 Jenkins 日志：`docker logs petcare-jenkins`
2. 查看构建日志：`/opt/petcare/infra/data/jenkins/jobs/petcare-deploy/builds/<build-number>/log`
3. 确认 Gitea 已同步最新代码
4. 清理 Jenkins workspace：`rm -rf /opt/petcare/infra/data/jenkins/workspace/petcare-deploy/*`

### 8.2 容器启动失败

1. 检查容器日志：`docker logs petcare-core`
2. 确认镜像存在：`docker images | grep pet-care`
3. 检查端口占用：`netstat -tulpn | grep 8080`
4. 验证环境变量配置

### 8.3 健康检查失败

1. 确认服务已启动：`docker ps | grep petcare`
2. 直接测试：`curl http://192.168.31.100:8080/actuator/health`
3. 检查应用日志
4. 增加等待时间

### 8.4 Maven 构建缓慢

1. 配置阿里云镜像（已在文档中说明）
2. 检查网络连接
3. 考虑使用本地 Maven 缓存

---

## 九、优化建议

### 9.1 短期优化

1. **配置 Jenkins 定时构建**：每 30 分钟自动检查并部署
2. **添加构建通知**：配置邮件或钉钉通知
3. **优化 Maven 缓存**：挂载本地 Maven 仓库到 Jenkins

### 9.2 长期优化

1. **实现蓝绿部署**：零停机更新
2. **添加自动化测试**：构建阶段运行单元测试
3. **配置回滚机制**：部署失败自动回滚
4. **监控告警**：配置 Grafana + Prometheus 告警规则
