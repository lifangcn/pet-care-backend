package pvt.mktech.petcare.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "pvt.mktech.petcare.user",
        "pvt.mktech.petcare.pet",
        "pvt.mktech.petcare.health",
        "pvt.mktech.petcare.reminder",
        "pvt.mktech.petcare.social",
        "pvt.mktech.petcare.infrastructure",
        "pvt.mktech.petcare.shared",
        "pvt.mktech.petcare.points"
})
@MapperScan(basePackages = {
        "pvt.mktech.petcare.user.mapper",
        "pvt.mktech.petcare.pet.mapper",
        "pvt.mktech.petcare.health.mapper",
        "pvt.mktech.petcare.reminder.mapper",
        "pvt.mktech.petcare.social.mapper",
        "pvt.mktech.petcare.points.mapper"
})
public class CoreServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreServiceApplication.class, args);
    }
}