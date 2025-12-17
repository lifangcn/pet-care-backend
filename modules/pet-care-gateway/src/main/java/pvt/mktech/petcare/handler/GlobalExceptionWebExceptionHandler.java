package pvt.mktech.petcare.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关全局异常处理器
 */
@Component
@Order(-2) // 优先级极高，捕获所有
public class GlobalExceptionWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 构建统一的错误响应体
        Map<String, Object> responseBody = new HashMap<>();
        HttpStatus status;

        if (ex instanceof IllegalArgumentException) {
            status = HttpStatus.BAD_REQUEST;
            responseBody.put("message", "请求参数错误: " + ex.getMessage());
        } else if ((ex.getMessage() != null && ex.getMessage().contains("JWT"))) {
            status = HttpStatus.UNAUTHORIZED;
            responseBody.put("message", "认证失败，请检查 Token");
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            responseBody.put("message", "网关内部错误");
        }
        responseBody.put("success", false);
        responseBody.put("path", exchange.getRequest().getURI().getPath());

        response.setStatusCode(status);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseBody);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}