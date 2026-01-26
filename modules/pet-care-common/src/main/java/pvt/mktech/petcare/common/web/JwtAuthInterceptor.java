package pvt.mktech.petcare.common.web;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pvt.mktech.petcare.common.jwt.JwtUtil;

import static pvt.mktech.petcare.common.constant.CommonConstant.HEADER_USER_ID;
import static pvt.mktech.petcare.common.constant.CommonConstant.TOKEN_HEADER;
import static pvt.mktech.petcare.common.constant.CommonConstant.TOKEN_PREFIX;

/**
 * {@code @description} JWT 认证拦截器，替代 Gateway 的 JwtAuthFilter
 * {@code @date} 2026-01-25
 * {@code @author} Michael
 */
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Token，优先从 Header中获取，其次从 URL 查询参数，用于 SSE 或 WebSocket
        String token = request.getHeader(TOKEN_HEADER);
        if (StrUtil.isNotBlank(token) && token.startsWith(TOKEN_PREFIX)) {
            token = token.substring(TOKEN_PREFIX.length());
        } else if (request.getRequestURI().endsWith("sse-connect")) {
            // 从 URL 查询参数获取 token（SSE 不支持自定义 Header）
            String query = request.getQueryString();
            if (query != null) {
                token = StrUtil.subAfter(query, "token=", false);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            request.setAttribute(HEADER_USER_ID, userId);
            return true;
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
