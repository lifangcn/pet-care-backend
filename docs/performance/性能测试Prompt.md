
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