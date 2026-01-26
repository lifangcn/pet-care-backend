package pvt.mktech.petcare.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code @description}: RocketMQ 配置类
 * {@code @date}: 2026/1/26
 * @author Michael
 */
@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server}")
    private String nameServer;

    @Value("${rocketmq.producer.group:core-reminder-producer}")
    private String producerGroup;

    @Value("${rocketmq.producer.send-message-timeout:3000}")
    private Integer sendMsgTimeout;

    @Value("${rocketmq.producer.retry-times-when-send-failed:2}")
    private Integer retryTimesWhenSendFailed;

    /**
     * 普通 Producer，用于发送普通消息和延迟消息
     */
    @Bean(destroyMethod = "shutdown")
    public DefaultMQProducer defaultMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendMsgTimeout);
        producer.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
        try {
            producer.start();
            log.info("RocketMQ 普通 Producer 启动成功, group: {}, nameServer: {}", producerGroup, nameServer);
        } catch (Exception e) {
            log.error("RocketMQ 普通 Producer 启动失败", e);
            throw new RuntimeException("RocketMQ Producer 启动失败", e);
        }
        return producer;
    }
}
