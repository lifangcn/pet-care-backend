package pvt.mktech.petcare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("pvt.mktech.petcare.mapper")
public class CoreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
        // TODO 使用 Redis Bitmap 实现用户签到和统计一个月的签到次数
        // TODO 使用 Redis HyperLogLog 实现UV统计
    }
}