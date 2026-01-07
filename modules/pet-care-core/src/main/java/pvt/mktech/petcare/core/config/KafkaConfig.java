package pvt.mktech.petcare.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    // 业务类型	    推荐方式        原因
    // 订单、支付	    同步发送        必须保证消息发送成功
    // 日志	        异步发送        高吞吐，允许少量丢失
    // 通知提醒	    同步+重试       重要但不是最关键
    // 数据同步	    异步+回调       数据量大，允许延迟

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        configProps.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        configProps.put(ACKS_CONFIG, "all");
        configProps.put(RETRIES_CONFIG, 2);
        configProps.put(ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory());
        log.info("KafkaTemplate initialized, bootstrap-servers: {}", bootstrapServers);
        return template;
    }
}