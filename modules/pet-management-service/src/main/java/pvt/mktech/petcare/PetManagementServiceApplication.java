package pvt.mktech.petcare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("pvt.mktech.petcare.mapper")
public class PetManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetManagementServiceApplication.class, args);
    }
}