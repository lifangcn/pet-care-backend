package pvt.mktech.petcare.club;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 宠物关怀-俱乐部 启动类。
 */
@SpringBootApplication
@MapperScan("pvt.mktech.petcare.club.mapper")
public class ClubServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClubServiceApplication.class, args);
    }
}
