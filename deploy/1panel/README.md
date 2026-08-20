# PetCare 1Panel v1 部署

该目录用于在低配 1Panel 服务器上运行 PostgreSQL 16 + pgvector、Redis、单节点 Kafka、Core 和 AI。Elasticsearch、MySQL 不属于此部署。

## 目录

服务器运行目录：

```text
/opt/1panel/apps/petcare/
├── app/releases/<release-id>/{pet-care-core.jar,pet-care-ai.jar}
├── app/current -> releases/<release-id>
├── backups/
├── conf/petcare.env              # 600，不进 Git
├── data/{postgres,redis,kafka}/
├── logs/{core,ai}/
├── postgres/init/01-bootstrap.sh
├── scripts/
├── compose.yml
└── Dockerfile.runtime
```

前端站点目录：

```text
/opt/1panel/apps/openresty/openresty/www/sites/michaelli.site/
├── index/                         # Vue dist 内容
└── .htpasswd                      # 不进 Git
```

1Panel v1 OpenResty 容器内的 `/www/sites/michaelli.site` 对应宿主机 `/opt/1panel/apps/openresty/openresty/www/sites/michaelli.site`。OpenResty 使用 `network_mode: host`，通过宿主回环地址 `127.0.0.1:8080/8081` 反代 Core/AI，不依赖 Docker 服务名，也无需加入 `1panel-network`。

Compose 仍将 Core/AI 加入外部 `1panel-network`，仅用于 1Panel 面板生态兼容。该网络必须使用与宿主机及 VPC 不重叠的 CIDR，例如 `10.250.0.0/24`。

## 本地构建

后端要求 JDK 21：

```bash
cd backend/pet-care-java
JAVA_HOME=/path/to/jdk-21 PATH="$JAVA_HOME/bin:$PATH" mvn clean test
JAVA_HOME=/path/to/jdk-21 PATH="$JAVA_HOME/bin:$PATH" mvn package -DskipTests
```

产物：

- `modules/pet-care-core/target/pet-care-core-1.0-SNAPSHOT.jar`
- `modules/pet-care-ai/target/pet-care-ai-1.0-SNAPSHOT.jar`

前端要求 Node `^20.19.0 || >=22.12.0`：

```bash
cd frontend/pet-care-vue
npm ci
npm run prod
```

生产前端基地址已设为 `/api`，上传 `dist/` 的内容到站点 `index/`。

## 首次准备

1. 将本目录同步到 `/opt/1panel/apps/petcare`。
2. 复制 `.env.example` 为 `conf/petcare.env`，逐项填写并执行 `chmod 600`。
3. 所有密码和令牌应独立生成，例如 `openssl rand -base64 48`。仓库历史中出现过的值视为已泄漏，禁止复用。
4. Kafka cluster ID 只生成一次，数据卷存在后禁止修改。
5. 创建 release 目录，复制两个 jar，原子更新 `app/current` 软链接。
6. 执行 `chmod +x postgres/init/01-bootstrap.sh scripts/*.sh`。

建议为 3.5GiB 主机增加 2GiB Swap（本项目已获得部署授权）：

```bash
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
sysctl vm.swappiness=10
sysctl vm.overcommit_memory=1
```

持久化 sysctl 时应写入单独的 `/etc/sysctl.d/99-petcare.conf`，不要覆盖系统文件。

## 首次启动门禁

```bash
cd /opt/1panel/apps/petcare
./scripts/preflight.sh
./scripts/deploy.sh
```

脚本按 PostgreSQL/Redis/Kafka → Kafka topics → Core/Flyway → AI 的顺序启动。首次启动后必须人工确认：

```bash
docker exec petcare-postgres psql -U postgres -d petcare -c '\dx'
docker exec petcare-postgres psql -U postgres -d petcare -c 'select version, success from petcare.flyway_schema_history order by installed_rank;'
docker compose --env-file conf/petcare.env -f compose.yml ps
docker logs --tail 200 petcare-core
docker logs --tail 200 petcare-ai
curl -fsS http://127.0.0.1:8080/error >/dev/null || true
curl -fsS http://127.0.0.1:8081/error >/dev/null || true
```

确认 AI 日志中没有 MySQL、Elasticsearch 或 Kafka 连接尝试。不要执行 `docker compose down -v`。

## 1Panel、域名与 HTTPS

1. DNS：`michaelli.site` 的 A 记录指向 `8.162.13.22`，删除错误 AAAA 后等待生效。
2. 在 1Panel 应用商店安装 OpenResty，并创建域名网站。
3. 确认 OpenResty 保持 1Panel v1 默认的 host 网络模式；Core/AI 的 `127.0.0.1:8080/8081` 发布端口应可从宿主访问。
4. 优先由 1Panel 申请并启用 HTTPS，使 80 端口只跳转到 HTTPS；再把 `openresty/michaelli.site.conf.template` 片段粘贴到面板维护的 HTTPS `server` 块内部。若使用独立 ACME 客户端，宿主机 webroot 对应 `/opt/1panel/apps/openresty/openresty/www/sites/michaelli.site/acme`。两种方式都必须保留模板中的 `/.well-known/acme-challenge/` Basic Auth 例外，并在续期后先执行 `openresty -t` 再 reload。不要把证书私钥写入仓库。
5. 生成 Basic Auth 文件，用户名和密码不要进入 Git：

   ```bash
   printf 'petcare:' > /opt/1panel/apps/openresty/openresty/www/sites/michaelli.site/.htpasswd
   openssl passwd -apr1 >> /opt/1panel/apps/openresty/openresty/www/sites/michaelli.site/.htpasswd
   chgrp <openresty-worker-group> /opt/1panel/apps/openresty/openresty/www/sites/michaelli.site/.htpasswd
   chmod 640 /opt/1panel/apps/openresty/openresty/www/sites/michaelli.site/.htpasswd
   ```

先在 OpenResty 容器内执行 `nginx -t` 确认 worker 能读取 `.htpasswd`。Basic Auth 仅保护静态入口和登录 bootstrap；业务 API 关闭 Basic Auth，继续使用应用 Bearer Token。否则两者会争用 `Authorization` 请求头。`/api/admin/ai/**`、`/api/internal/**` 和 `/api/actuator/**` 均直接拒绝公网访问。

安全组/防火墙只开放 22、80、443。5432、6379、9092、8080、8081 不对公网开放。

## 备份与回滚

```bash
./scripts/backup.sh
./scripts/rollback.sh <release-id>
```

备份包含 PostgreSQL custom dump、角色定义、Redis RDB、Compose 和 SHA-256。应再复制一份到异机或 OSS，并实际演练恢复。

回滚脚本只切换 Core/AI release 并重建应用容器，**不会回滚 Flyway 数据库迁移**。数据库恢复属于停机高风险操作，必须人工确认后执行。

## 已知边界

- 这是单机部署，不具备主机级高可用。
- Core 仍依赖 Kafka；Kafka 单节点故障可能丢失未消费消息。
- 演示验证码和 mock 微信登录仅在 `AUTH_DEMO_ENABLED=true` 时开放，必须保留登录入口保护。
- AI 管理接口首次上线被反代封禁，待完整管理员授权链实现后再开放。
- 当前应用没有 Actuator，容器 healthcheck 仅证明 Java 端口已监听；业务冒烟仍不可省略。
