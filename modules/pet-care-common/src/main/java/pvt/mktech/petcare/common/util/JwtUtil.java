
package pvt.mktech.petcare.common.util;

import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;



/**
 * JWT工具类，用于生成和解析JWT令牌
 */
@Slf4j
@Component
public class JwtUtil {
    public static final Long LOGIN_TOKEN_TTL = 3600L;
    public static final String JWT_SECRET = "petcare-jwt-secret-key-2025-must-be-at-least-256-bits-long-for-hmac-sha256-algorithm";
    /**
     * 使用JWT_SECRET初始化的密钥对象
     */
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    @Value("${jwt.secret:petcare-secret-key-2024}")
    private String secret;

    @Value("${jwt.expiration:86400}")
    private Long expiration; // 默认24小时

    /**
     * 生成JWT令牌
     *
     * @param userId 用户ID
     * @param username 用户名
     * @return 生成的JWT令牌字符串
     */
    public static String generateToken(Long userId, String username) {
        Date now = DateUtil.date();
        Date expiryDate = new Date(now.getTime() + LOGIN_TOKEN_TTL * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))           // 设置用户ID为主题
                .claim("username", username)            // 添加用户名声明
                .issuedAt(now)                             // 设置签发时间
                .expiration(expiryDate)                    // 设置过期时间
                .signWith(SECRET_KEY)                      // 使用密钥签名
                .compact();                                // 构建并返回紧凑格式的JWT
    }

    /**
     * 解析JWT令牌
     *
     * @param token JWT令牌字符串
     * @return 解析后的Claims对象
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)                    // 验证密钥
                .build()                                   // 构建解析器
                .parseSignedClaims(token)                  // 解析已签名的声明
                .getPayload();                             // 获取载荷部分
    }

    /**
     * 从JWT令牌中获取用户ID
     *
     * @param token JWT令牌字符串
     * @return 用户ID
     */
    public static Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());       // 从主题中获取用户ID
    }

    /**
     * 从JWT令牌中获取用户名
     *
     * @param token JWT令牌字符串
     * @return 用户名
     */
    public static String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);      // 从声明中获取用户名
    }

    /**
     * 检查JWT令牌是否已过期
     *
     * @param token JWT令牌字符串
     * @return 如果令牌已过期返回true，否则返回false
     */
    public static boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date()); // 比较过期时间和当前时间
        } catch (Exception e) {
            return true;                                      // 解析异常也认为是过期
        }
    }
}