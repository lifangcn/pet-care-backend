# 认证重构方案：移除旧 JWT，统一 Sa-Token

> **Goal:** 移除旧 JWT 手写体系（JwtUtil + JwtAuthInterceptor），认证全链路归一到 Sa-Token，消除代码冗余。
>
> **Architecture:** Sa-Token JWT Simple 模式已接管认证（SaInterceptor + StpLogicJwtForSimple），旧 JWT 代码仅作为 Spring Bean 空转但从未被调用。本次重构纯做减法：删除废弃文件、清理残留引用、统一配置。不新增任何文件。
>
> **Tech Stack:** Sa-Token 1.34+（JWT Simple 模式）、Spring Boot 3.3、Redis（Refresh Token 存储）

---

## 问题诊断

项目处于「旧 JWT 手写方案」→「Sa-Token JWT 方案」迁移的半途状态，具体 6 个问题：

### 问题 1：两套 Token 体系并存，职责重叠

| 组件 | 模块 | 实际状态 |
|------|------|---------|
| `JwtUtil.java` | common | 可用，但**已经没人在调用** |
| `JwtAutoConfiguration.java` | common | 通过 SPI 注册，`jwt.enabled=true` 只是让它空转 |
| `JwtAuthInterceptor.java` | common | `@Component` 注册了，但**未被任何 WebMvcConfigurer 注册到拦截链** |
| `UserInfoInterceptor.java` | common | 同上，`@Component` 存在但未被使用 |
| `SaTokenConfig.java` | core | **实际生效**的认证拦截器 |

### 问题 2：UserContext 的双重获取逻辑

`UserContext.getUserId()` 先从 ThreadLocal 取，取不到再 fallback 到 `StpUtil.isLogin()`。这个 fallback 掩盖了 ThreadLocal 未被正确设置的 bug（SaTokenConfig 拦截器已经保证每次请求都 setUserId）。

### 问题 3：Refresh Token 自造轮子

AuthServiceImpl 用 Redis + UUID 手搓了 Refresh Token 管理。Sa-Token JWT Simple 模式是无状态的（JWT 不存 Redis），无法原生支持刷新时让旧 Token 失效。**保留 Redis 管理 Refresh Token 是合理的**，但应通过 `StpUtil.logout(userId)` 让旧 Access Token 失效。

### 问题 4：配置冗余

application.yml 中同时存在 `jwt.*` 和 `sa-token.*` 两套配置，实际上只有 `sa-token` 在生效。

### 问题 5：WebMvcAutoConfiguration 职责模糊

注释写着「认证已由 Sa-Token 统一处理」，但 `@ComponentScan` 仍在扫描废弃的拦截器类。

### 问题 6：pet-care-common 同时引入 sa-token-core 和 jjwt-*

jjwt 三件套只被 JwtUtil 使用，JwtUtil 废弃后应一并移除。

---

## 变更清单

| 操作 | 文件路径（相对项目根目录） | 模块 |
|------|--------------------------|------|
| 🗑 删除 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtUtil.java` | common |
| 🗑 删除 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtAutoConfiguration.java` | common |
| 🗑 删除 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/JwtAuthInterceptor.java` | common |
| 🗑 删除 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserInfoInterceptor.java` | common |
| 🗑 删除 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/WebMvcProperties.java` | common |
| ✏️ 修改 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserContext.java` | common |
| ✏️ 修改 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/WebMvcAutoConfiguration.java` | common |
| ✏️ 修改 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/constant/CommonConstant.java` | common |
| ✏️ 修改 | `modules/pet-care-common/pom.xml` | common |
| ✏️ 修改 | `modules/pet-care-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | common |
| ✏️ 修改 | `modules/pet-care-core/src/main/java/pvt/mktech/petcare/user/service/impl/AuthServiceImpl.java` | core |
| ✏️ 修改 | `modules/pet-care-core/src/main/resources/application.yml` | core |
| ✏️ 修改 | `modules/pet-care-core/src/main/resources/application-local.example.yml` | core |
| ✏️ 修改 | `modules/pet-care-ai/src/main/java/pvt/mktech/petcare/chat/controller/ChatController.java` | ai |
| ✏️ 修改 | `modules/pet-care-ai/src/main/resources/application.yml` | ai |
| ✏️ 修改 | `modules/pet-care-ai/src/main/resources/application-local.example.yml` | ai |

**共计**：删除 5 个文件，修改 11 个文件，**零新增文件**。

---

## Task 1：删除旧 JWT 废弃文件（5 个文件）

### 删除文件

1. `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtUtil.java`
2. `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtAutoConfiguration.java`
3. `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/JwtAuthInterceptor.java`
4. `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserInfoInterceptor.java`
5. `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/WebMvcProperties.java`

### 安全性验证

- `JwtUtil` 仅被 `JwtAuthInterceptor` 引用（一起删除）
- `JwtAutoConfiguration` 仅在 `AutoConfiguration.imports` 中引用（Task 6 清理）
- `JwtAuthInterceptor` 无业务代码 import，未被任何 WebMvcConfigurer 注册
- `UserInfoInterceptor` 同上，未被注册到拦截链
- `WebMvcProperties` 仅在 pet-care-ai 的 `application.yml` 中有配置引用（Task 9 清理）

---

## Task 2：清理 UserContext.java

**文件**：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserContext.java`

**改动说明**：移除 `StpUtil` fallback，只保留纯粹的 ThreadLocal 读写。移除后 `UserContext` 不再依赖 `sa-token-core`。

**改动前**（完整文件内容）：

```java
package pvt.mktech.petcare.common.web;

import cn.dev33.satoken.stp.StpUtil;

/**
 * {@code @description}: 用户信息上下文（支持 Servlet ThreadLocal 和 WebFlux Reactor Context）
 * {@code @date}: 2025/12/17 08:50
 *
 * @author Michael
 */
public class UserContext {

    public static final String USER_INFO_KEY = "USER_Id";

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // Servlet 环境使用
    public static void setUserId(Long userId) {
        threadLocal.set(userId);
    }

    public static Long getUserId() {
        // 从 ThreadLocal 获取
        Long userId = threadLocal.get();
        if (userId != null) {
            return userId;
        }
        // 从 SA-Token 获取
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception e) {
            // SA-Token 未初始化或其他异常，忽略
        }
        return null;
    }

    public static void removeUserId() {
        threadLocal.remove();
    }
}
```

**改动后**（完整文件内容）：

```java
package pvt.mktech.petcare.common.web;

/**
 * {@code @description}: 用户信息上下文（ThreadLocal）
 * {@code @date}: 2025/12/17 08:50
 * @author Michael
 */
public class UserContext {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        threadLocal.set(userId);
    }

    public static Long getUserId() {
        return threadLocal.get();
    }

    public static void removeUserId() {
        threadLocal.remove();
    }
}
```

---

## Task 3：清理 WebMvcAutoConfiguration.java

**文件**：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/WebMvcAutoConfiguration.java`

**改动说明**：移除 `@ComponentScan`（不再需要扫描已删除的拦截器类），移除未使用的 import。

**改动前**（完整文件内容）：

```java
package pvt.mktech.petcare.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 自动配置
 * 注意：认证拦截器已由 Sa-Token 统一处理，此处仅处理用户上下文清理
 * {@code @date} 2026-01-25
 * {@code @author} Michael
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ComponentScan(basePackages = "pvt.mktech.petcare.common.web")
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户上下文清理拦截器（请求结束后清理 ThreadLocal）
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                        Object handler, Exception ex) {
                UserContext.removeUserId();
            }
        }).order(Integer.MAX_VALUE);

        log.info("WebMvcAutoConfiguration 初始化完成（Sa-Token 统一认证模式）");
    }
}
```

**改动后**（完整文件内容）：

```java
package pvt.mktech.petcare.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 自动配置（用户上下文清理）
 * 认证拦截器由各模块的 SaTokenConfig 统一处理
 * {@code @date} 2026-01-25
 * @author Michael
 */
@Slf4j
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                        Object handler, Exception ex) {
                UserContext.removeUserId();
            }
        }).order(Integer.MAX_VALUE);

        log.info("WebMvcAutoConfiguration 初始化完成");
    }
}
```

---

## Task 4：清理 CommonConstant.java

**文件**：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/constant/CommonConstant.java`

**改动说明**：移除 JWT 相关的 3 个常量。`TOKEN_HEADER` 和 `TOKEN_PREFIX` 仅被已删除的 `JwtAuthInterceptor` 引用。`HEADER_USER_ID` 被已删除的 `JwtAuthInterceptor`、`UserInfoInterceptor` 和 pet-care-ai 的 `ChatController.getUserIdFromRequest()`（Task 9 一并清理为死代码）引用。

**改动前**：

```java
package pvt.mktech.petcare.common.constant;

public class CommonConstant {
    // DistributedIdGenerator 相关
    public static final String ID_PREFIX = "id:generator:";
    public static final long BEGIN_TIMESTAMP = 1735689600L; // 开始时间戳2025-01-01 00:00:00
    public static final int COUNT_BITS = 32;

    // JWT相关
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_USER_ID = "X-User-Id";
    // 限流
    public static final String RATE_LIMIT_KEY = "rate:limiter:api:";
}
```

**改动后**：

```java
package pvt.mktech.petcare.common.constant;

public class CommonConstant {
    public static final String ID_PREFIX = "id:generator:";
    public static final long BEGIN_TIMESTAMP = 1735689600L;
    public static final int COUNT_BITS = 32;

    public static final String RATE_LIMIT_KEY = "rate:limiter:api:";
}
```

---

## Task 5：清理 pom.xml 依赖

### 文件 1：`modules/pet-care-common/pom.xml`

移除 jjwt 三件套依赖：

```xml
<!-- 删除以下 3 个 dependency -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <scope>runtime</scope>
</dependency>
```

保留 `sa-token-core` 依赖不变（`UserContext` 仍可能被其他模块间接使用）。

### 文件 2：`modules/pet-care-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**改动前**：

```
pvt.mktech.petcare.common.jwt.JwtAutoConfiguration
pvt.mktech.petcare.common.storage.FileStorageAutoConfiguration
pvt.mktech.petcare.common.redis.RedisAutoConfiguration
pvt.mktech.petcare.common.web.WebMvcAutoConfiguration
pvt.mktech.petcare.common.exception.ExceptionAutoConfiguration
```

**改动后**：

```
pvt.mktech.petcare.common.storage.FileStorageAutoConfiguration
pvt.mktech.petcare.common.redis.RedisAutoConfiguration
pvt.mktech.petcare.common.web.WebMvcAutoConfiguration
pvt.mktech.petcare.common.exception.ExceptionAutoConfiguration
```

---

## Task 6：重构 AuthServiceImpl — Refresh Token 优化

**文件**：`modules/pet-care-core/src/main/java/pvt/mktech/petcare/user/service/impl/AuthServiceImpl.java`

**改动说明**：
1. `generateTokens()` — 增加 `StpUtil.getTokenSession()` 关联 Refresh Token，用 `StpUtil.getTokenTimeout()` 获取实际过期时间
2. `refreshToken()` — 刷新时先 `StpUtil.logout(userId)` 让旧 Access Token 失效
3. 移除 `ACCESS_TOKEN_TTL` 常量（改用 `StpUtil.getTokenTimeout()` 动态获取）

### 改动 1：常量区域

**改动前**：

```java
private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
private static final long ACCESS_TOKEN_TTL = 86400L;
private static final long REFRESH_TOKEN_TTL = 604800L;
```

**改动后**：

```java
private static final String REFRESH_TOKEN_PREFIX = "auth:refresh_token:";
private static final long REFRESH_TOKEN_TTL = 604800L;
```

### 改动 2：generateTokens 方法

**改动前**：

```java
/**
 * 生成双 Token（Access Token + Refresh Token）
 * Access Token: JWT 格式，短期有效，用于接口访问
 * Refresh Token: UUID 格式，长期有效，存储在 Redis，用于刷新 Access Token
 */
private void generateTokens(LoginInfoDto loginInfoDto) {
    Long userId = loginInfoDto.getId();

    // Sa-Token 登录，生成 JWT 格式的 Access Token
    StpUtil.login(userId);
    String accessToken = StpUtil.getTokenValue();

    // 生成 Refresh Token（UUID 格式，存储在 Redis）
    String refreshToken = RandomUtil.randomString(32);
    String refreshTokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
    redisUtil.set(refreshTokenKey, userId.toString(), Duration.ofSeconds(REFRESH_TOKEN_TTL));

    loginInfoDto.setAccessToken(accessToken);
    loginInfoDto.setRefreshToken(refreshToken);
    loginInfoDto.setExpiresIn(ACCESS_TOKEN_TTL);

    log.debug("用户登录成功，userId: {}, accessToken 已生成", userId);
}
```

**改动后**：

```java
/**
 * 生成双 Token（Access Token + Refresh Token）
 * Access Token: Sa-Token JWT，由 Sa-Token 管理
 * Refresh Token: UUID + Redis，长期有效，用于刷新 Access Token
 *
 * @param loginInfoDto 登录信息 DTO
 * @author Michael Li
 * @since 2026-03-29
 */
private void generateTokens(LoginInfoDto loginInfoDto) {
    Long userId = loginInfoDto.getId();

    StpUtil.login(userId);
    String accessToken = StpUtil.getTokenValue();

    String refreshToken = RandomUtil.randomString(32);
    String refreshTokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
    redisUtil.set(refreshTokenKey, userId.toString(), Duration.ofSeconds(REFRESH_TOKEN_TTL));

    StpUtil.getTokenSession().set("refreshToken", refreshToken);

    loginInfoDto.setAccessToken(accessToken);
    loginInfoDto.setRefreshToken(refreshToken);
    loginInfoDto.setExpiresIn(StpUtil.getTokenTimeout());

    log.debug("用户登录成功，userId: {}", userId);
}
```

### 改动 3：refreshToken 方法

**改动前**：

```java
@Override
public LoginInfoDto refreshToken(LoginInfoDto dto) {
    String refreshToken = dto.getRefreshToken();
    if (StrUtil.isBlank(refreshToken)) {
        throw new BusinessException(ErrorCode.TOKEN_INVALID);
    }

    // 从 Redis 获取 Refresh Token 对应的用户 ID
    String refreshTokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
    String userIdStr = redisUtil.get(refreshTokenKey);
    if (StrUtil.isBlank(userIdStr)) {
        throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    }

    Long userId = Long.parseLong(userIdStr);

    // 删除旧的 Refresh Token
    redisUtil.delete(refreshTokenKey);

    // 查询用户信息
    User user = getById(userId);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    // 生成新的双 Token
    LoginInfoDto newLoginInfo = new LoginInfoDto();
    BeanUtil.copyProperties(user, newLoginInfo);
    generateTokens(newLoginInfo);

    log.info("Token 刷新成功，userId: {}", userId);
    return newLoginInfo;
}
```

**改动后**：

```java
/**
 * 刷新 Token
 *
 * @param dto 包含 refreshToken 的 DTO
 * @return 新的登录信息
 * @author Michael Li
 * @since 2026-03-29
 */
@Override
public LoginInfoDto refreshToken(LoginInfoDto dto) {
    String refreshToken = dto.getRefreshToken();
    if (StrUtil.isBlank(refreshToken)) {
        throw new BusinessException(ErrorCode.TOKEN_INVALID);
    }

    String refreshTokenKey = REFRESH_TOKEN_PREFIX + refreshToken;
    String userIdStr = redisUtil.get(refreshTokenKey);
    if (StrUtil.isBlank(userIdStr)) {
        throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
    }

    Long userId = Long.parseLong(userIdStr);
    redisUtil.delete(refreshTokenKey);

    User user = getById(userId);
    if (user == null) {
        throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    StpUtil.logout(userId);

    LoginInfoDto newLoginInfo = new LoginInfoDto();
    BeanUtil.copyProperties(user, newLoginInfo);
    generateTokens(newLoginInfo);

    log.info("Token 刷新成功，userId: {}", userId);
    return newLoginInfo;
}
```

### 改动 4：logout 方法（无逻辑变化，仅补充注释）

```java
/**
 * 用户登出
 *
 * @param dto 登录信息 DTO
 * @author Michael Li
 * @since 2026-03-29
 */
@Override
public void logout(LoginInfoDto dto) {
    try {
        if (dto != null && StrUtil.isNotBlank(dto.getRefreshToken())) {
            String refreshTokenKey = REFRESH_TOKEN_PREFIX + dto.getRefreshToken();
            redisUtil.delete(refreshTokenKey);
        }
        StpUtil.logout();
        log.info("用户登出成功");
    } catch (Exception e) {
        log.warn("登出失败", e);
    }
}
```

---

## Task 7：清理 pet-care-core 配置文件

### 文件 1：`modules/pet-care-core/src/main/resources/application.yml`

**删除以下配置块**：

```yaml
# 删除整个 jwt 配置块
jwt:
  enabled: true
  expire-time: 86400
  refresh-expire-time: 604800
```

Sa-Token 配置块保留不变（它已经在生效）：

```yaml
sa-token:
  token-name: Authorization
  timeout: 86400
  active-timeout: -1
  is-concurrent: true
  is-share: false
  is-log: true
  token-prefix: Bearer
  is-read-header: true
  is-read-cookie: false
  is-write-header: true
```

### 文件 2：`modules/pet-care-core/src/main/resources/application-local.example.yml`

**删除**：

```yaml
# -------------------------------------------------------------------------
# JWT 密钥配置
# -------------------------------------------------------------------------
jwt:
  # 生成密钥: openssl rand -base64 32
  secret-key: "your_jwt_secret_key_base64_encoded"
```

**修改末尾注释**，将：

```yaml
# 5. JWT: 用户认证签名密钥，必须配置（sa-token.jwt-secret-key 与 jwt.secret-key 保持一致）
```

改为：

```yaml
# 5. SA-Token: 用户认证签名密钥（sa-token.jwt-secret-key）
```

---

## Task 8：清理 pet-care-ai 模块

### 文件 1：`modules/pet-care-ai/src/main/java/pvt/mktech/petcare/chat/controller/ChatController.java`

**删除 import**：

```java
// 删除这行 import
import pvt.mktech.petcare.common.constant.CommonConstant;
```

**删除死代码方法**（约 L122-L135，没有任何地方调用它）：

```java
// 删除整个方法
private Long getUserIdFromRequest(ServerHttpRequest request) {
    String userIdHeader = request.getHeaders().getFirst(CommonConstant.HEADER_USER_ID);
    if (userIdHeader != null && !userIdHeader.isEmpty()) {
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    return null;
}
```

### 文件 2：`modules/pet-care-ai/src/main/resources/application.yml`

**删除以下配置块**：

```yaml
# 删除整个 jwt 配置块
jwt:
  enabled: true
  secret-key: ${JWT_SECRET_KEY}
  expire-time: 86400
  refresh-expire-time: 604800

# 删除整个 web.mvc 配置块（旧 JwtAuthInterceptor 的路径配置，从未生效）
web:
  mvc:
    jwt-include-paths:
      - /ai/document/**
      - /ai/chat/**
      - /ai/sync/**
```

### 文件 3：`modules/pet-care-ai/src/main/resources/application-local.example.yml`

**删除**：

```yaml
# -------------------------------------------------------------------------
# JWT 密钥配置（需与 pet-care-core 保持一致）
# -------------------------------------------------------------------------
jwt:
  # 生成密钥: openssl rand -base64 32
  secret-key: "your_jwt_secret_key_base64_encoded"
```

**修改末尾注释**，将：

```
# 7. JWT: 用户认证签名密钥，需与 pet-care-core 保持一致
```

改为：

```
# 7. SA-Token: 用户认证由 pet-care-core 统一处理
```

---

## 重构后的认证链路

```
┌─────────────── pet-care-core ───────────────────┐
│                                                  │
│  HTTP 请求                                       │
│    ↓                                             │
│  SaInterceptor（SaTokenConfig, order=0）          │
│    ├── /auth/**        → 放行                     │
│    ├── /swagger-ui/**  → 放行                     │
│    ├── /doc.html       → 放行                     │
│    ├── /actuator/**    → 放行                     │
│    ├── SSE (sse-connect) → URL token 参数校验     │
│    │       StpUtil.getLoginIdByToken(token)       │
│    └── 其他请求 → StpUtil.checkLogin()            │
│    ↓                                             │
│  UserContext.setUserId(userId)                   │
│    ↓                                             │
│  业务 Controller → UserContext.getUserId()        │
│    ↓                                             │
│  WebMvcAutoConfiguration.afterCompletion          │
│  → UserContext.removeUserId()  (order=MAX)       │
│                                                  │
└──────────────────────────────────────────────────┘

┌─────────────── pet-care-ai ─────────────────────┐
│                                                  │
│  请求由 Nginx/Core 转发                           │
│  → UserContext.setUserId() 由上游处理             │
│  → 业务 Controller → UserContext.getUserId()     │
│  → afterCompletion → UserContext.removeUserId()  │
│                                                  │
└──────────────────────────────────────────────────┘
```

**一条链路，一个入口，一个出口，零冗余。**

---

## 验证清单

重构完成后执行以下验证：

```bash
# 1. 编译检查
mvn clean compile

# 2. 全量测试
mvn test

# 3. 启动 core 模块，验证以下接口
cd modules/pet-care-core && mvn spring-boot:run

# 4. 验证登录流程
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d '{"phone":"...","code":"..."}'
# 预期：返回 accessToken（JWT 格式）和 refreshToken

# 5. 验证接口鉴权
curl http://localhost:8080/user/info
# 预期：401 未认证

curl http://localhost:8080/user/info -H "Authorization: Bearer <accessToken>"
# 预期：200 正常返回用户信息
```

---

## 风险评估

| 风险项 | 等级 | 说明 | 应对 |
|--------|------|------|------|
| 删除 JwtUtil 后编译报错 | 低 | 已确认无业务代码引用 | 编译验证 |
| UserContext 移除 fallback 后空指针 | 低 | SaTokenConfig 保证每次请求都 set | 关注 AI 模块是否有未拦截的请求路径 |
| Refresh Token 刷新逻辑变更 | 中 | 新增了 `StpUtil.logout(userId)` | 测试刷新流程，确认旧 Token 失效 |
| pet-care-ai 移除 jwt 配置 | 低 | 这些配置从未生效过 | 启动 AI 模块验证 |

---

## 后续可选优化（不在本次范围内）

1. **Sa-Token 升级到 Mixin 模式**：如果后续需要踢人下线、同端互斥登录等特性，将 `StpLogicJwtForSimple` 改为 `StpLogicJwtForMixin`（JWT 存 Redis，有状态）
2. **pet-care-ai 添加独立 Sa-Token 拦截器**：当前 AI 模块没有自己的认证拦截器，如果 AI 独立部署（不经 Core 转发），需要添加
3. **Sa-Token SSO 单点登录**：如果后续需要多个前端应用共享登录态
