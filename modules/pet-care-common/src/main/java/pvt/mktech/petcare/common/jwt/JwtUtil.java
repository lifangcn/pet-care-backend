package pvt.mktech.petcare.common.jwt;

import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import java.util.Date;

import static pvt.mktech.petcare.common.constant.CommonConstant.HEADER_USERNAME;

/**
 * JWT 工具类
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtUtil {

    private String secretKey;
    private Long expireTime;
    private Long refreshExpireTime;

    // 生成 Token
    public String generateAccessToken(String username, Long userId) {
        Date now = DateUtil.date();
        Date expiryDate = new Date(now.getTime() + (expireTime * 1000));
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(HEADER_USERNAME, username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSignInKey())
                .compact();
    }

    // 生成刷新Token
    public String generateRefreshToken(Long userId) {
        Date now = DateUtil.date();
        Date expiryDate = DateUtil.offsetSecond(now, refreshExpireTime.intValue());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .expiration(expiryDate)
                .signWith(getSignInKey())
                .compact();
    }

    // 解析并验证 Token
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 获取用户 ID
    public Long getUserIdFromToken(String token) {
        return Long.valueOf(getClaimsFromToken(token).getSubject());
    }

    // 获取用户名
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).get(HEADER_USERNAME, String.class);
    }

    // 解析令牌
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 校验 Token 是否有效
    public boolean validateToken(String token) {
        try {

            parseToken(token);
            return true;
        } catch (Exception e) {
            // 解析失败（过期、签名错误等）
            return false;
        }
    }

    // 刷新令牌验证（专门验证refresh token）
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}