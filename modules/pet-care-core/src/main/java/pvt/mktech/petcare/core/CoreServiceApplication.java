package pvt.mktech.petcare.core;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import pvt.mktech.petcare.common.config.CommonComponentConfig;

@SpringBootApplication
@Import(CommonComponentConfig.class)
@MapperScan("pvt.mktech.petcare.core.mapper")
@EnableDubbo
public class CoreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
        // TODO 使用 Redis Bitmap 实现用户签到和统计一个月的签到次数
        // TODO 使用 Redis HyperLogLog 实现UV统计
    }
}