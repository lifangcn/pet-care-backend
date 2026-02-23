package pvt.mktech.petcare.user.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import pvt.mktech.petcare.points.service.PointsService;

import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_USER_REGISTER_CONSUMER;
import static pvt.mktech.petcare.infrastructure.constant.CoreConstant.CORE_USER_REGISTER_TOPIC;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegisterConsumer {
    private final PointsService pointsService;

    @KafkaListener(topics = CORE_USER_REGISTER_TOPIC, groupId = CORE_USER_REGISTER_CONSUMER,
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key, Acknowledgment acknowledgment) {
        long userId = Long.parseLong(message);
        pointsService.grantRegisterPoints(userId);
        acknowledgment.acknowledge();
    }
}
