# 线程池压测完整方案

## 1. 压测目标

验证线程池参数配置是否合理，通过压测找到最优配置：

| 目标 | 说明 | 验证方法 |
|------|------|----------|
| **吞吐量** | 系统最大处理能力 | TPS/QPS 峰值 |
| **响应时间** | 用户等待体验 | P95、P99 延迟 |
| **资源利用率** | 服务器资源使用情况 | CPU 70%~80% 目标 |
| **稳定性** | 长时间运行表现 | 拒绝率、错误率 |

---

## 2. 压测工具选型

### 2.1 主流工具对比

| 工具 | 准确性 | 资源消耗 | 学习成本 | 适用场景 |
|------|--------|----------|----------|----------|
| **JMeter** | 中 | 高 | 低 | 中小规模、快速验证 |
| **Gatling** | 高 | 中 | 中 | 高并发、代码驱动 |
| **K6** | 高 | 低 | 低 | 云原生、CI/CD |
| **Locust** | 高 | 中 | 中 | Python 技术栈 |
| **wrk2** | 高 | 极低 | 中 | HTTP 接口精准测试 |
| **自研压测工具** | 最高 | 可控 | 高 | 全链路压测 |

### 2.2 推荐方案

**小型测试（开发阶段）：**
```
wrk2 + JVM 监控
```
- 轻量、精准
- 直接暴露线程池监控端点

**中型测试（测试环境）：**
```
Gatling/K6 + Prometheus + Grafana
```
- 代码定义场景，可复用
- 完善的监控体系

**生产环境压测：**
```
全链路压测平台（自研/阿里云 PTS）+ 影子库
```
- 流量隔离
- 真实场景验证

---

## 3. 线程池核心监控指标

### 3.1 ThreadPoolExecutor 原生指标

```java
// 核心监控数据
threadPool.getPoolSize()           // 当前线程池大小
threadPool.getActiveCount()        // 活跃线程数
threadPool.getQueue().size()       // 队列任务数
threadPool.getCompletedTaskCount() // 已完成任务数
threadPool.getLargestPoolSize()    // 历史最大线程数
threadPool.getTaskCount()          // 总任务数
```

### 3.2 关键性能指标（KPI）

| 指标 | 健康阈值 | 告警阈值 | 说明 |
|------|----------|----------|------|
| **CPU 利用率** | 60%~70% | > 85% | 过高说明线程数不足或计算密集 |
| **队列积压** | < 100 | > 500 | 持续积压说明处理能力不足 |
| **活跃线程率** | 50%~70% | > 90% | activeCount / poolSize |
| **任务拒绝率** | 0% | > 1% | RejectedExecutionException 次数 |
| **任务等待时间** | < 100ms | > 500ms | 队列中等待时间 |
| **任务执行时间** | < 1s | > 3s | 平均执行耗时 |
| **线程创建峰值** | ≤ maxPoolSize | > maxPoolSize | 说明最大线程数配置偏小 |

### 3.3 监控实现

**方式一：Spring Boot Actuator**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**方式二：自定义监控端点**
```java
@RestController
public class ThreadPoolMonitorController {

    @GetMapping("/actuator/threadpool")
    public Map<String, Object> getThreadPoolStats() {
        // 返回各线程池状态
    }
}
```

---

## 4. 压测方法论

### 4.1 压测类型

| 类型 | 目的 | 并发策略 | 持续时间 |
|------|------|----------|----------|
| **基准测试** | 建立性能基线 | 逐步递增 | 10 分钟 |
| **负载测试** | 找到系统容量 | 梯度加压 | 30 分钟 |
| **压力测试** | 找出性能上限 | 持续高压 | 60 分钟 |
| **稳定性测试** | 验证长时间运行 | 模拟真实流量 | 24 小时 |

### 4.2 加压策略

```
阶段式加压（推荐）：
10 → 50 → 100 → 200 → 500 → 1000 → ...
每阶段持续 10 分钟，观察指标稳定后进入下一阶段

坡道式加压：
从 10 开始，每分钟增加 10%，持续 30 分钟

尖峰测试：
瞬间达到 3x 预期峰值，验证系统弹性
```

### 4.3 压测场景设计

**场景 1：纯计算任务**
- 模拟 CPU 密集型操作
- 验证核心线程数配置

**场景 2：IO 等待任务**
- 模拟数据库查询、HTTP 调用
- 验证最大线程数配置

**场景 3：混合任务**
- 真实业务场景模拟
- 验证整体配置合理性

---

## 5. 线程池参数调优流程

### 5.1 初始配置

根据任务类型计算初始值：

| 任务类型 | corePoolSize | maximumPoolSize | queueCapacity |
|----------|--------------|-----------------|---------------|
| CPU 密集型 | N | N + 1 | SynchronousQueue |
| IO 密集型 | 2N | 4N | 500 |

### 5.2 调优决策树

```
开始压测
    ↓
CPU 利用率 < 50%？
    ├─ 是 → 增加线程数 → 重新压测
    └─ 否 → 继续判断
              ↓
          队列持续积压？
              ├─ 是 → 增加最大线程数 → 重新压测
              └─ 否 → 继续判断
                        ↓
                    有任务拒绝？
                        ├─ 是 → 增加队列容量或线程数 → 重新压测
                        └─ 否 → 配置合理 ✓
```

### 5.3 判断标准

| 现象 | 原因 | 调优方向 |
|------|------|----------|
| CPU 低，队列满 | 线程数不足 | 增加最大线程数 |
| CPU 高，响应慢 | 线程数过多 | 减少核心线程数 |
| 频繁拒绝 | 队列或线程不足 | 增加队列容量或线程数 |
| 队列积压严重 | 处理速度慢 | 优化任务逻辑或增加线程 |

---

## 6. 压测环境准备

### 6.1 环境要求

| 环境 | 配置建议 | 说明 |
|------|----------|------|
| **开发环境** | CPU 4 核，内存 8G | 快速验证 |
| **测试环境** | 与生产同配置 | 准确评估 |
| **预发环境** | 生产 1:1 复制 | 最终验证 |
| **生产环境** | 需要隔离方案 | 全链路压测 |

### 6.2 数据准备

```bash
# 基础数据量：与生产保持一致
# 热点数据：覆盖 80% 查询场景
# 压测专用账号：避免影响真实用户
```

### 6.3 监控搭建

```yaml
# Prometheus 采集 JVM 和线程池指标
# Grafana 展示压测仪表盘
# 告警规则：CPU > 85%, 拒绝率 > 1%
```

---

## 7. wrk2 精准压测方案

### 7.1 为什么选择 wrk2

- 比 JMeter 更轻量、更精准
- 支持 LATENCY 分布统计
- 能够精确控制 RPS（每秒请求数）
- 适合线程池参数验证

### 7.2 安装

```bash
# macOS
brew install wrk

# Linux
git clone https://github.com/giltene/wrk2.git
cd wrk2 && make
```

### 7.3 压测脚本示例

```lua
-- thread_pool_test.lua
wrk.method = "POST"
wrk.body   = '{"task":"test"}'
wrk.headers["Content-Type"] = "application/json"

-- 不同压测阶段的 RPS 配置
init = function()
    rps = 100  -- 起始 RPS
end

request = function()
    wrk.headers["X-Request-Id"] = tostring(math.random(1, 1000000))
    return wrk.format(nil, nil, nil, nil)
end

done = function(summary, latency, requests)
    print("请求总数: " .. summary.requests)
    print("平均延迟: " .. latency.mean)
    print("P99 延迟: " .. latency:percentile(99))
end
```

### 7.4 执行命令

```bash
# 10 秒预热 + 60 秒压测，100 RPS
./wrk -t10 -c100 -d60s -R100 -s thread_pool_test.lua http://localhost:8080/api/test

# 同时监控 JVM
jstat -gcutil <pid> 1000 60
```

### 7.5 结果分析

```
重点观察：
1. Latency P99 是否随 RPS 增加而急剧上升
2. 在某个 RPS 下出现 Non-2xx 响应（拒绝任务）
3. JVM GC 是否频繁
4. 结合线程池监控端点数据
```

---

## 8. 全链路压测（生产环境）

### 8.1 核心原理

```
真实流量 ───┐
            ├─→ 网关（识别压测标识）→ 业务逻辑
压测流量 ───┘                                ↓
                                      [影子库判断]
                                          ↓
                    ┌─────────────────────┴─────────────────────┐
                    ↓                                           ↓
               生产库（真实流量）                        影子库（压测流量）
```

### 8.2 关键技术

| 技术 | 实现方式 |
|------|----------|
| **流量标识** | 请求头 `X-Pressure-Test: true` |
| **数据隔离** | 影子库/表，按标识路由 |
| **Mock 外部调用** | 拦截第三方服务，返回模拟数据 |
| **压测开关** | 配置中心动态控制 |
| **流量回放** | 记录生产流量，定时回放 |

### 8.3 实施步骤

```
1. 业务链路梳理
2. 影子库表结构创建
3. 压测标识透传机制
4. 数据隔离逻辑实现
5. 小流量验证（1%）
6. 逐步放量（10% → 50% → 100%）
7. 监控数据分析
8. 容量评估报告
```

---

## 9. 压测报告模板

### 9.1 基本信息

| 项目 | 内容 |
|------|------|
| **压测对象** | ai-service 线程池 |
| **压测环境** | 测试环境（8 核 16G） |
| **压测时间** | 2026-02-03 14:00 ~ 16:00 |
| **压测工具** | wrk2 + Prometheus |
| **初始配置** | core=16, max=32, queue=500 |

### 9.2 压测结果

| 指标 | 压测前 | 压测后（优化后） | 结论 |
|------|--------|-----------------|------|
| **最大 TPS** | 500 | 1200 | 提升 140% |
| **P99 延迟** | 850ms | 180ms | 降低 79% |
| **CPU 利用率** | 45% | 72% | 趋于合理 |
| **拒绝率** | 2.5% | 0% | 消除拒绝 |
| **队列积压** | 800 | 50 | 消除积压 |

### 9.3 最优配置

```yaml
thread-pool:
  ai-service:
    core-size: 20      # 调优后
    max-size: 40       # 调优后
    queue-capacity: 200 # 调优后
    keep-alive: 60s
```

### 9.4 建议

1. 保持当前配置，观察生产环境表现
2. 设置监控告警：队列积压 > 200 时告警
3. 下次压测周期：3 个月后或业务增长 50% 时

---

## 10. 注意事项

### 10.1 常见误区

| 误区 | 正确做法 |
|------|----------|
| 追求最大 TPS | 关注合理 TPS 下的延迟 |
| 只测峰值 | 全场景覆盖（低、中、高负载） |
| 忽略队列大小 | 队列容量与线程数一起调优 |
| 一次测试定论 | 多次测试取平均值 |
| 只看平均值 | 关注 P95、P99 长尾延迟 |

### 10.2 安全提醒

```yaml
压测前检查:
  - 备份测试数据库
  - 确认压测开关关闭（生产环境）
  - 准备回滚方案
  - 通知相关人员

压测中监控:
  - 系统资源（CPU、内存、磁盘）
  - 应用日志（异常、错误）
  - 中间件状态（数据库、Redis）

压测后清理:
  - 清理测试数据
  - 关闭压测工具
  - 整理监控数据
```

---

## 11. Sources

- [The Thread Pool Tuning Everyone Copies Is Useless in 2025](https://medium.com/lets-code-future/the-thread-pool-tuning-everyone-copies-is-useless-in-2025-ed7b70810045)
- [Java Performance Testing: Best Practices & Tools](https://bell-sw.com/blog/java-performance-testing-best-practices-tools/)
- [Java Thread Pool Implementation and Best Practices](https://www.alibabacloud.com/blog/601528)
- [Java load testing: Why Gatling is the tool for Java developers](https://gatling.io/blog/java-load-testing)
- [美团全链路压测自动化实践](https://tech.meituan.com/2019/02/14/full-link-pressure-test-automation.html)
- [字节跳动全链路压测(Rhino)的实践](https://juejin.cn/post/6882548316463169543)
- [B站在全链路压测上的实践](https://www.bilibili.com/read/cv17581400)
- [大型系统高可用压测体系建设](https://cloud.tencent.com/developer/article/2478597)
- [主流的性能测试工具盘点](https://blog.csdn.net/F36_9_/article/details/148292366)
- [阿里云 PTS - 压测工具对比](https://help.aliyun.com/zh/pts/product-overview/comparison-among-stress-testing-tools)
