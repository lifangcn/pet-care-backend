package pvt.mktech.petcare.gateway.loader;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;

import java.util.concurrent.Executor;

/**
 * {@code @description}: 动态路由加载器
 * TODO 动态拉取Nacos路由配置文件 gateway-routes.json
 * {@code @date}: 2025/12/17 11:09
 *
 * @author Michael
 */
//@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicRouteLoader {
    // Nacos配置管理器
    private final NacosConfigManager nacosConfigManager;
    // 路由定义写入器
    private final RouteDefinitionWriter routeDefinitionWriter;

    private final String dataId = "gateway-routes.json";
    private final String group = "DEFAULT_GROUP";

    @PostConstruct
    public void load() {
        log.info("TODO 开始加载动态路由...");
        try {
            nacosConfigManager.getConfigService().getConfigAndSignListener(dataId, group, 5000, new Listener() {
                @Override
                public Executor getExecutor() {
                    // 获取线程池执行
                    return null;
                }

                @Override
                public void receiveConfigInfo(String s) {
                    log.info("删除旧路由，并添加新路由。更新路由需要一定时间执行");
                }
            });
        } catch (NacosException e) {
            log.error("Nacos异常：{}", e.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }
}
