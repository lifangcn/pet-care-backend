package pvt.mktech.petcare.gateway.loader;

import cn.hutool.json.JSONUtil;
import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.stereotype.Component;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.exception.SystemException;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * {@code @description}: 动态路由加载器
 * {@code @date}: 2025/12/17 11:09
 *
 * @author Michael
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicRouteLoader {
    // Nacos配置管理器
    private final NacosConfigManager nacosConfigManager;
    // 路由定义写入器
    private final RouteDefinitionWriter routeDefinitionWriter;
    // 保存路由信息
    private final Set<String> routeIds = new HashSet<>();

    private static final String GATEWAY_ROUTES_CONFIG_DATA_ID = "gateway-routes.json";
    private static final String GATEWAY_ROUTES_CONFIG_GROUP = "DEFAULT_GROUP";

    @PostConstruct
    public void load() {
        try {
            String configInfo = nacosConfigManager.getConfigService()
                    .getConfigAndSignListener(GATEWAY_ROUTES_CONFIG_DATA_ID, GATEWAY_ROUTES_CONFIG_GROUP, 5000, new Listener() {
                @Override
                public Executor getExecutor() {
                    // 获取线程池执行
                    return null;
                }

                @Override
                public void receiveConfigInfo(String configInfo) {
                    log.info("删除旧路由，并添加新路由。更新路由需要一定时间执行");
                    updateConfigInfo(configInfo);
                }
            });
            // 首次启动加载路由
            updateConfigInfo(configInfo);
        } catch (NacosException e) {
            log.error("Nacos异常：{}", e.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    // 更新路由
    private void updateConfigInfo(String configInfo) {
        List<RouteDefinition> routeDefinitionList = JSONUtil.toList(configInfo, RouteDefinition.class);
        for (String routeId : routeIds) {
            routeDefinitionWriter.delete(Mono.just(routeId)).subscribe();
        }
        routeIds.clear();
        // 更新路由
        for (RouteDefinition routeDefinition : routeDefinitionList) {
            routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
            routeIds.add(routeDefinition.getId());
        }
    }
}
