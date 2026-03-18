package pvt.mktech.petcare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
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