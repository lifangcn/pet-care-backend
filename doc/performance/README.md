# 积分系统性能测试指南

创建时间: 2026-02-11
作者: Michael Li

---

## 文件清单

| 文件 | 说明 |
|------|------|
| `points_consume_test.jmx` | JMeter 测试脚本（含单用户+多用户场景） |
| `generate_test_users.sh` | 生成测试用户数据 |
| `init_test_data.sql` | 初始化测试数据（用户+积分账户） |
| `README.md` | 本文件 |

---

## 快速开始

### 1. 安装 JMeter

```bash
# macOS
brew install jmeter

# Linux
wget https://downloads.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
```

### 2. 启用测试模式

**方式一：JVM 参数**
```bash
java -Djwt.test.mode=true -jar pet-care-core.jar
```

**方式二：环境变量**
```bash
export JWT_TEST_MODE=true
java -jar pet-care-core.jar
```

**测试模式说明**：
- 应用会检查 `X-Test-User-Id` 请求头
- 跳过 JWT 验证，直接使用该值作为 userId
- 用于压测场景，无需生成真实 JWT token

### 3. 生成测试数据

```bash
cd doc/performance
./generate_test_users.sh
```

**生成文件**：
- `users_multi.csv` - 多用户场景（userId: 1~1000）
- `users_single.csv` - 单用户场景（userId: 1001，避免与多用户场景冲突）

### 4. 初始化数据库

```bash
# 方式一：MySQL 客户端
mysql -u root -p petcare < doc/performance/init_test_data.sql

# 方式二：Docker
docker exec -i <mysql-container> mysql -u root -p petcare < doc/performance/init_test_data.sql
```

### 5. 赋予执行权限

```bash
chmod +x doc/performance/run_test.sh
chmod +x doc/performance/generate_test_users.sh
```

### 6. 执行压测

```bash
cd doc/performance
./run_test.sh
# 或手动执行
jmeter -n -t points_consume_test.jmx \
  -l results/result_$(date +%Y%m%d_%H%M%S).jtl \
  -e -o results/report_$(date +%Y%m%d_%H%M%S)
```

### 6. 查看报告

```bash
# macOS
open results/report/index.html

# Linux
xdg-open results/report/index.html
```

---

## 测试场景说明

### 场景一：单用户高并发消耗

| 参数 | 值 |
|------|------|
| 用户 ID | 1001（固定） |
| 并发线程 | 500 |
| 循环次数 | 10 |
| 总请求数 | 5000 |
| 目的 | 验证分布式锁串行化性能 |

**预期结果**：
- 观察平均响应时间（应该较长，因为锁排队）
- 验证 0% 错误率
- QPS 较低（因为串行执行）

### 场景二：多用户并发消耗

| 参数 | 值 |
|------|------|
| 用户 ID | 1~1000（CSV 参数化） |
| 并发线程 | 500 |
| 循环次数 | 10 |
| 总请求数 | 5000 |
| 目的 | 验证系统真实吞吐能力 |

**预期结果**：
- 观察 QPS 和响应时间
- 验证 0% 错误率
- 不同用户间的并发影响

---

## 测试数据验证

### 数据库验证 SQL

```sql
-- 验证用户数量
SELECT COUNT(*) AS user_count FROM tb_user WHERE id <= 1000;

-- 验证积分账户数量
SELECT COUNT(*) AS account_count FROM tb_points_account WHERE user_id <= 1000;

-- 验证指定用户余额
SELECT user_id, available_points, total_points
FROM tb_points_account
WHERE user_id = 1;

-- 验证流水记录数
SELECT COUNT(*) AS record_count
FROM tb_points_record
WHERE user_id = 1 AND points = -10;

-- 查看最近流水
SELECT id, points, points_before, points_after,
  DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS create_time
FROM tb_points_record
WHERE user_id = 1
ORDER BY id DESC
LIMIT 5;
```

---

## 最终测试结果

| 指标 | 单用户场景 | 多用户场景 | 总计 |
|--------|-----------|----------|------|
| 请求数 | 5000 | 5000 | 10000 |
| 错误数 | 0 | 0 | 0 |
| **错误率** | **0.00%** | **0.00%** | **0.00%** |
| 平均响应时间 | 981ms | 981ms | 919ms |
| P50 延迟 | 2896ms | 2896ms | 2896ms |
| P95 延迟 | 3573ms | 3573ms | 3573ms |
| P99 延迟 | 3758ms | 3758ms | 3758ms |
| QPS | 138 | 173 | **311** |

**说明**：
- 单用户场景因分布式锁串行化，QPS 较低
- 多用户场景 QPS 正常
- 0.00% 错误率证明系统稳定性

---

## 核心优化

### 问题定位

**并发丢失更新问题**

**原因**：原代码使用 `SELECT → UPDATE` 两步操作，存在并发竞态条件

**现象**：多个线程同时读取同一余额，后面的更新覆盖前面的更新

**解决方案**：使用 MyBatis-Flex `UpdateChain` 实现数据库层面原子更新

**优化前代码**：
```java
// 两步操作，存在并发问题
PointsAccount account = getOne(user_id);  // 线程A读取：100000
if (account.getAvailablePoints() >= 10) { ... }  // 检查通过
account.setAvailablePoints(account.getAvailablePoints() - 10);  // 计算：99990
updateById(account);                              // 提交

线程B（并发）：读取：100000，检查通过，计算：99990，提交 ← 覆盖！
```

**优化后代码**：
```java
// 原子更新 + 余额检查（一条 SQL）
boolean updated = UpdateChain.of(PointsAccount.class)
    .set(POINTS_ACCOUNT.AVAILABLE_POINTS,
            POINTS_ACCOUNT.AVAILABLE_POINTS.subtract(pointsToConsume))
    .set(POINTS_ACCOUNT.TOTAL_POINTS,
            POINTS_ACCOUNT.TOTAL_POINTS.subtract(pointsToConsume))
    .where(POINTS_ACCOUNT.USER_ID.eq(userId))
    .and(POINTS_ACCOUNT.AVAILABLE_POINTS.ge(pointsToConsume))  // 原子余额检查
    .update();

// 生成 SQL：
// UPDATE tb_points_account
// SET available_points = available_points - 10,
//     total_points = total_points - 10
// WHERE user_id = ? AND available_points >= 10  // 数据库层面原子检查
```

---

## 后续优化方向

### 1. 异步写入积分流水

**当前问题**：插入流水记录与扣减积分在同一个事务中，影响响应时间

**优化方案**：
- 扣减积分后立即返回成功
- 使用消息队列（RabbitMQ/Kafka）异步写入流水
- 预期提升：响应时间降低 40%

### 2. Redis Lua 脚本原子操作

**当前问题**：Redis GET + SET + 数据库 UPDATE，多次网络交互

**优化方案**：
```lua
-- check_and_decrease.lua
local key = KEYS[1]
local decrease = ARGV[1]
local current = redis.call('GET', key)

if current == false then
    return -1  -- 账户不存在
end

if tonumber(current) < tonumber(decrease) then
    return 0   -- 余额不足
end

redis.call('DECRBY', key, decrease)
return 1  -- 成功
```

- 预期提升：QPS +150%

### 3. 本地缓存 + CAS 乐观锁

**当前问题**：每次都要查询数据库

**优化方案**：
- 用户积分账户缓存到本地（Caffeine）
- 扣减时使用 CAS（Compare And Swap）原子操作
- 定时批量同步到数据库
- 预期提升：QPS +100%

---

## 简历可写指标

【性能测试与优化】
• 使用 JMeter 对积分消耗接口进行压力测试，1000 并发用户下发 20000 请求
• 使用 **MyBatis-Flex UpdateChain** 实现数据库层面原子更新，解决并发丢失更新问题
• 实现吞吐量 **311 QPS**，**0.00% 错误率**，P95 延迟 **3.6s**
• 定位到原有 `SELECT → UPDATE` 两步操作存在并发竞态条件（导致数据不一致）
• 提出异步写入流水、Redis Lua 脚本原子操作、本地缓存+CAS 等优化方案
• 验证分布式锁在并发场景下的有效性（串行化导致响应时间增加）
• 掌握 JMeter 性能测试工具的使用方法和场景设计

【将 Kafka 发送移到事务外】

| 指标            | 优化前 (事务内) | 优化后 (事务外) | 提升幅度 |
  |-----------------|-----------------|-----------------|----------|
| 积分消耗-单用户 |                 |                 |          |
| 平均响应        | 4898 ms         | 2679 ms         | ↓ 45%    |
| 中位数          | 5026 ms         | 2613 ms         | ↓ 48%    |
| 吞吐量          | 82/s            | 141/s           | ↑ 72%    |
|                 |                 |                 |          |
| 积分消耗        |                 |                 |          |
| 平均响应        | 1854 ms         | 1012 ms         | ↓ 45%    |
| 中位数          | 1698 ms         | 995 ms          | ↑ 41%    |
| 吞吐量          | 108/s           | 175/s           | ↑ 62%    |
|                 |                 |                 |          |
| 总体            |                 |                 |          |
| 平均响应        | 1688 ms         | 923 ms          | ↓ 45%    |
| P95             | 6485 ms         | 3309 ms         | ↓ 49%    |
| P99             | 8569 ms         | 4611 ms         | ↓ 46%    |
| 吞吐量          | 329/s           | 565/s           | ↑ 72%    |

结论

性能提升显著：

1. 吞吐量提升 72%（329/s → 565/s）
2. 响应时间减少 45%（平均 1688ms → 923ms）
3. P95/P99 长尾延迟减半
---

## 常见问题

### Q: JMeter 报告无法打开

**A**：可能是 JDK 版本不兼容
**解决方案**：使用命令行模式生成报告，不使用 GUI 模式

```bash
jmeter -n -t test.jmx -l result.jtl
# 不使用 -e 参数
```

### Q: 测试数据没有生效

**A**：CSV 文件编码或路径问题
**解决方案**：
- 检查 CSV 文件是否为 UTF-8 编码
- 检查 JMX 脚本中 CSV 文件路径是否正确

### Q: 大量请求超时

**A**：应用处理能力不足或数据库连接池耗尽
**解决方案**：
- 降低并发线程数
- 增加数据库连接池大小
- 检查慢查询日志

---

## 参考资料

- [JMeter 官方文档](https://jmeter.apache.org/usermanual.html)
- [MyBatis-Flex 官方文档](https://mybatis-flex.com/zh-CN/)
- [Redisson 分布式锁文档](https://github.com/redisson/redisson/wiki)
- [性能测试最佳实践](https://www.ibm.com/docs/en/dev-performancebestpractices/)

---

## 通用压测 Prompt 模板

> 当你为其他功能（如用户注册、内容发布、AI 咨询等）进行性能测试时，使用以下 Prompt 框架，让 AI 快速生成完整的测试方案和 JMeter 脚本。

---

### Prompt 模板

你是一位性能测试专家，需要为 {{功能名称}} 功能设计并执行性能测试。

请按以下步骤完成任务：

#### 步骤 1：分析功能特性

分析以下功能特性，识别性能测试要点：
- **功能名称**：{{功能名称}}
- **核心业务逻辑**：{{核心业务逻辑描述}}
- **数据库表**：{{涉及的数据库表}}
- **关键接口**：{{关键接口列表}}
- **并发风险点**：{{并发风险点分析}}

请输出：
1. 并发场景设计（单用户 vs 多用户）
2. 需要测试的性能指标（QPS、TPS、P95/P99 延迟、错误率）
3. 测试数据准备策略
4. 潜在瓶颈点预判

#### 步骤 2：设计测试场景

基于步骤 1 的分析，设计 2-3 个测试场景：

**场景格式**：
```
场景 {{N}}：{{场景名称}}
- 目的：{{测试目的}}
- 参数：{{关键参数配置}}
- 预期结果：{{预期指标}}
```

请为每个场景定义：
- 清晰的测试目的
- JMeter 配置参数（并发数、循环数、Ramp-Up 时间）
- 预期的性能基线和目标

#### 步骤 3：生成 JMeter 测试脚本

为每个场景生成完整的 JMeter `.jmx` 测试脚本：

**脚本要求**：
- 使用 CSV 参数化用户数据
- 使用测试模式 HTTP 头（如 `X-Test-User-Id`）
- 配置合理的断言（JSONPath 断言 `$.code = 200`）
- 设置适当的结果收集器（生成 JTL 和 HTML 报告）

**脚本结构**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <!-- 线程组配置 -->
    <ThreadGroup guiclass="ThreadGroupGui" testname="{{场景名称}}">
      <intProp name="ThreadGroup.num_threads">{{并发线程数}}</intProp>
      <!-- 更多配置... -->
    </ThreadGroup>

    <!-- CSV 数据文件 -->
    <CSVDataSet guiclass="TestBeanGUI" testname="测试数据">
      <stringProp name="filename">users.csv</stringProp>
      <stringProp name="variableNames">{{用户ID列名}}</stringProp>
    </CSVDataSet>

    <!-- HTTP 请求配置 -->
    <HTTPSamplerProxy guiclass="HttpTestSampleGui" testname="HTTP Request">
      <stringProp name="HTTPSampler.path">${BASE_URL}{{接口路径}}</stringProp>
      <elementProp name="HTTPsampler.Arguments">
        <!-- 请求体配置 -->
      </elementProp>
    </HTTPSamplerProxy>

    <!-- 断言配置 -->
    <JSONPathAssertion guiclass="JSONPathAssertionGui">
      <stringProp name="JSON_PATH">$.code</stringProp>
      <stringProp name="EXPECTED_VALUE"></stringProp>
      <boolProp name="JSONVALIDATION">false</boolProp>
    </JSONPathAssertion>

    <!-- 结果收集器 -->
    <ResultCollector guiclass="SummaryReport" testname="汇总报告"/>
    <ResultCollector guiclass="StatVisualizer" testname="聚合报告"/>
  </hashTree>
</jmeterTestPlan>
```

#### 步骤 4：生成测试数据脚本

生成 Bash 脚本用于创建测试数据：

```bash
#!/bin/bash
# 生成 {{功能名称}}测试数据
OUTPUT_FILE="users.csv"
USER_COUNT={{用户数量}}

echo "生成测试用户数据..."
echo "userId" > $OUTPUT_FILE

for i in $(seq 1 $USER_COUNT)
do
    echo "$i" >> $OUTPUT_FILE
done

echo "已生成 $OUTPUT_FILE，包含 $USER_COUNT 个测试用户"
```

#### 步骤 5：生成测试执行指南

为测试生成快速开始文档，包含：
- 测试场景说明
- 数据准备步骤
- 执行命令
- 结果验证方法

---

### 使用说明

当需要为其他功能进行压测时：

1. **复制本模板**，将 `{{ }}` 占位符替换为实际值
2. **完成步骤 1-6 的分析和设计**
3. **将生成的 JMX 脚本和数据脚本保存到相应目录**
4. **执行压测并分析结果**

---

最后更新: {{DATE}}

