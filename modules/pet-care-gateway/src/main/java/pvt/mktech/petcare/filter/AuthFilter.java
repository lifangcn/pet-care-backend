package pvt.mktech.petcare.filter;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;
import pvt.mktech.petcare.common.util.JwtUtil;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * {@code @description}: 鉴权过滤器
 * {@code @date}: 2025/12/12 18:50
 *
 * @author Michael
 */
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    // 白名单路径
    private static final Set<String> WHITE_LIST = Set.of(
            "/v1/auth/code",
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/auth/captcha"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 白名单直接放行
        if (WHITE_LIST.contains(path)) {
            return chain.filter(exchange);
        }

        // 获取token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
//        if (StrUtil.isBlank(token) || !token.startsWith("Bearer ")) {
//            return unauthorized(exchange);
//        }

//        token = token.substring(7);
        // 获取真实的客户端IP（通过Nginx传递）
        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            exchange.getAttributes().put("CLIENT_IP", realIp);
        }
        // 添加请求ID到响应头
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("X-Request-ID", exchange.getRequest().getId());

        try {
            Claims claims = JwtUtil.parseToken(token);
            // 将用户信息添加到header中传递给下游服务
            exchange = exchange.mutate()
                    .request(builder -> builder
                            .header("X-User-Id", claims.get("userId", String.class))
                            .header("X-Username", claims.getSubject()))
                    .build();
            return chain.filter(exchange);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }


    @Override
    public int getOrder() {
        return 0;
    }
}
