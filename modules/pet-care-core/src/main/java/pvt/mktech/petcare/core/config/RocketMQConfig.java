package pvt.mktech.petcare.core.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static pvt.mktech.petcare.core.constant.CoreConstant.*;

@Slf4j
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name-server:127.0.0.1:9876}")
    private String nameServer;

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public DefaultMQProducer reminderProducer() throws MQClientException {
        DefaultMQProducer producer = new DefaultMQProducer(CORE_REMINDER_PRODUCER);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(3000);
        producer.setRetryTimesWhenSendFailed(2);
        producer.setRetryTimesWhenSendAsyncFailed(2);
        log.info("RocketMQ Producer initialized: {}", CORE_REMINDER_PRODUCER);
        return producer;
    }

}

