package pvt.mktech.petcare.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerBeanPostProcessorAutoConfiguration;
import org.springframework.context.annotation.Import;
import pvt.mktech.petcare.common.config.CommonComponentConfig;

@SpringBootApplication(exclude = {
        LoadBalancerAutoConfiguration.class,
        LoadBalancerBeanPostProcessorAutoConfiguration.class
})
@Import(CommonComponentConfig.class)
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
