# Kafka to RocketMQ 迁移文档

## 一、Kafka 与 RocketMQ 优缺点对比

| 维度 | Kafka | RocketMQ | 选型理由 |
|------|-------|----------|----------|
| **架构** | 依赖ZooKeeper，架构复杂 | 独立部署，无外部依赖 | RocketMQ更轻量化 |
| **性能** | 极高吞吐量，适合海量数据 | 高吞吐量，适合亿级消息 | 项目规模中等，两者均满足 |
| **延迟** | 毫秒级 | 微秒级 | RocketMQ更低延迟 |
| **延迟消息** | 不支持，需额外组件 | **5.x支持任意延迟时间** | **项目核心需求** |
| **运维复杂度** | 需维护ZK+Kafka集群 | 单独维护Broker | RocketMQ更简单 |
| **消息可靠性** | 多副本机制 | 同步/异步复制 | 两者均可靠 |
| **消费模式** | Pull模式为主 | Push+Pull混合 | RocketMQ更灵活 |
| **社区活跃度** | 国际活跃 | 国内活跃（阿里） | 均有成熟支持 |

**选型结论**：选择RocketMQ 5.x，主要原因是：
1. **任意精度延迟消息**：5.x版本支持毫秒级精度的自定义延迟时间，完美匹配提醒业务
2. **架构更简单**：无需额外维护ZooKeeper集群
3. **运维成本低**：单一组件部署和运维

## 二、改动内容汇总

### 2.1 依赖变更
- 移除：`spring-kafka`
- 新增：`rocketmq-client:5.3.2`

### 2.2 代码变更
| 文件 | 操作 | 说明 |
|------|------|------|
| `KafkaConfig.java` | 删除 | 不再需要 |
| `ReminderDelayQueueWorker.java` | 删除 | 使用RocketMQ延迟消息替代Redis ZSet扫描 |
| `RocketMQConfig.java` | 新增 | Producer配置类 |
| `ReminderScanScheduler.java` | 修改 | 生产者从KafkaTemplate改为DefaultMQProducer |
| `ReminderPendingConsumer.java` | 修改 | 实现ApplicationListener手动启动Consumer，使用@Transactional本地事务 |
| `ReminderSendConsumer.java` | 修改 | 实现ApplicationListener手动启动Consumer |

### 2.3 配置文件变更
- `application.yml`

变更内容：
- 移除`kafka.*`配置
- 新增`rocketmq.*`配置

## 三、详细代码变更

### 3.1 pom.xml
```xml
<!-- 旧：Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- 新：RocketMQ 5.x -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-client</artifactId>
    <version>5.3.2</version>
</dependency>
```

**注意**：不使用`rocketmq-spring-boot-starter`，直接使用原生`rocketmq-client`，以便精确控制Producer配置（如事务消息）。

### 3.2 新增配置类（RocketMQConfig.java）
```java
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer defaultMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer("core-reminder-producer");
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(3000);
        producer.setRetryTimesWhenSendFailed(2);
        producer.start();
        return producer;
    }
}
```

### 3.3 生产者变更（ReminderScanScheduler.java）
```java
// 旧：Kafka
import org.springframework.kafka.core.KafkaTemplate;
private final KafkaTemplate<String, String> kafkaTemplate;
kafkaTemplate.send(CORE_REMINDER_TOPIC_PENDING, key, value).get();

// 新：RocketMQ
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
private final DefaultMQProducer defaultMQProducer;

Message message = new Message(CORE_REMINDER_PENDING_TOPIC, null, key, value.getBytes(StandardCharsets.UTF_8));
defaultMQProducer.send(message);
```

### 3.4 消费者变更（ReminderPendingConsumer.java）
```java
// 旧：Kafka
@KafkaListener(topics = CORE_REMINDER_TOPIC_PENDING, groupId = CORE_REMINDER_PENDING_CONSUMER,
        containerFactory = "kafkaListenerContainerFactory")
public void consume(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key, Acknowledgment acknowledgment) {
    processMessage(messageDto);
    acknowledgment.acknowledge();
}

// 新：RocketMQ 原生API
@Slf4j
@Component
public class ReminderPendingConsumer implements ApplicationListener<ApplicationReadyEvent> {

    @Resource
    private DefaultMQProducer defaultMQProducer;

    private DefaultMQPushConsumer consumer;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        consumer = new DefaultMQPushConsumer(CORE_REMINDER_PENDING_CONSUMER);
        consumer.setNamesrvAddr(nameServer);
        consumer.subscribe(CORE_REMINDER_PENDING_TOPIC, "*");
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                    processMessage(body);
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
    }

    /**
     * 使用 @Transactional 保证本地事务（创建 execution、更新 reminder）
     * 消息发送在事务提交后执行
     */
    @Transactional(rollbackFor = Exception.class)
    public void processMessage(String message) {
        // DB 操作...
        sendDelayMessage(execution);
    }
}
```

### 3.5 RocketMQ 5.x 延迟消息实现
```java
// 计算延迟时间（毫秒）
long delayMillis = Duration.between(LocalDateTime.now(), execution.getNotificationTime()).toMillis();

// 构造消息
Message message = new Message(CORE_REMINDER_SEND_TOPIC, body.getBytes(StandardCharsets.UTF_8));

if (delayMillis > 0) {
    // 设置延迟时间（RocketMQ 5.x 支持任意毫秒级精度）
    message.setDelayTimeMs(delayMillis);
}
defaultMQProducer.send(message);
```

**优势**：
- 无需计算延迟等级
- 支持任意时间跨度（秒、分、小时、天）
- 毫秒级精度

## 四、配置文件变更

### 4.1 application.yml
```yaml
# 旧：Kafka
kafka:
  bootstrap-servers: localhost:9092

# 新：RocketMQ
rocketmq:
  name-server: localhost:9876
  producer:
    group: core-reminder-producer
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
  consumer:
    consume-thread-min: 1
    consume-thread-max: 1
```

### 4.2 日志配置
```yaml
# 旧
org.apache.kafka: WARN
org.springframework.kafka: WARN

# 新
org.apache.rocketmq: WARN
```

## 五、架构变化

### 5.1 旧架构（Kafka + Redis ZSet）
```
ReminderScanScheduler --Kafka--> ReminderPendingConsumer --Redis ZSet--> ReminderDelayQueueWorker --Kafka--> ReminderSendConsumer
```

### 5.2 新架构（RocketMQ 5.x 延迟消息）
```
ReminderScanScheduler --RocketMQ--> ReminderPendingConsumer --RocketMQ任意延迟--> ReminderSendConsumer
```

**优化点**：
1. 移除了Redis ZSet作为中间延迟队列
2. 移除了ReminderDelayQueueWorker定时扫描任务
3. 使用RocketMQ 5.x任意精度延迟消息，架构更简洁

**事务处理**：
- `ReminderPendingConsumer` 使用 `@Transactional` 保证本地事务一致性
- DB 操作（创建 execution、更新 reminder）与消息发送在同一事务中
- 消息发送在事务提交后执行，确保数据一致性

## 六、部署说明

### 6.1 RocketMQ 5.x 服务端部署
```bash
# Docker方式部署 NameServer
docker run -d -p 9876:9876 \
  -v /data/rocketmq/logs:/home/rocketmq/logs \
  --name rmqserver \
  apache/rocketmq:5.3.1 \
  mqnamesrv

# Docker方式部署 Broker（需开启延迟消息功能）
docker run -d -p 10911:10911 -p 10909:10909 \
  -v /data/rocketmq/broker/logs:/home/rocketmq/logs \
  -v /data/rocketmq/broker/store:/home/rocketmq/store \
  -v /data/rocketmq/broker/conf:/home/rocketmq/conf \
  --name rmqbroker \
  --link rmqserver:namesrv \
  -e "NAMESRV_ADDR=namesrv:9876" \
  apache/rocketmq:5.3.1 \
  mqbroker -c /home/rocketmq/conf/broker.conf
```

### 6.2 Broker配置（启用延迟消息）
```properties
# broker.conf
enablePropertyFilter=true
enableDelayTimeOperation=true  # 5.x启用任意延迟时间功能
```

### 6.3 Topic创建
```bash
# 创建Pending Topic
sh mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t CORE_REMINDER_PENDING

# 创建Send Topic
sh mqadmin updateTopic -n localhost:9876 -c DefaultCluster -t CORE_REMINDER_SEND
```

## 七、注意事项

1. **服务端版本**：Broker必须为5.x版本且开启`enableDelayTimeOperation`
2. **消费者组**：修改消费者组名称后需先删除旧的订阅关系
3. **延迟时间上限**：理论上无上限，实际建议不超过30天
4. **事务处理**：使用 `@Transactional` 本地事务，DB 操作与消息发送保持一致性
5. **不使用 rocketmq-spring-boot-starter**：直接使用原生 rocketmq-client，便于精确控制配置

## 八、验证步骤

1. 启动RocketMQ 5.x服务
2. 创建Topic
3. 启动应用，检查日志是否成功连接NameServer
4. 创建测试提醒（设置未来1天、3天、7天的提醒时间）
5. 验证是否在精确时间点收到提醒消息
