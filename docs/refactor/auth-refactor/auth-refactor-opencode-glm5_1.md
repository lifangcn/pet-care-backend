# 身份验证系统统一重构设计文档

> 日期：2026-03-29
> 作者：Michael Li
> 状态：待执行

---

## 一、现状问题分析

### 1.1 两套 JWT 系统并存

项目存在两套 JWT 认证系统，其中 Legacy 系统是死代码（从未被运行时调用）：

| 系统 | 实际状态 | 用途 |
|------|---------|------|
| Legacy JWT (jjwt) | 死代码，未注册拦截器 | `JwtUtil` + `JwtAuthInterceptor` |
| Sa-Token JWT Simple | 实际生效 | `SaTokenConfig` + `StpUtil` |

Legacy 系统虽未注册到拦截器链，但仍通过 `@ComponentScan` 被实例化为 Spring Bean，`JwtAutoConfiguration` 仍通过 `AutoConfiguration.imports` 加载。

### 1.2 AI 模块无认证保护

`pet-care-ai` 模块（独立进程部署在 8081 端口）没有 `SaInterceptor`，也没有任何认证拦截器。其 `UserContext.getUserId()` 依赖 Sa-Token 的 fallback 机制，但 AI 模块的 pom 中没有 `sa-token-jwt` 依赖，JWT secret key 也未统一配置。**AI 模块接口完全裸奔**。

### 1.3 `NotLoginException` 未被全局异常处理器捕获

`GlobalExceptionHandler` 没有 `@ExceptionHandler(NotLoginException.class)`。Sa-Token 拦截器抛出的未登录异常会落入通用 `RuntimeException` 处理器，返回通用系统错误而非 401 认证错误。

### 1.4 `UserContext.getUserId()` 的 Sa-Token fallback 掩盖问题

`UserContext.getUserId()` 在 ThreadLocal 为空时会 fallback 到 `StpUtil.isLogin()`。这让忘记设置 ThreadLocal 的地方也能"碰巧"工作，掩盖了潜在的拦截器配置错误，尤其在 AI 模块中。

### 1.5 配置重复与安全隐患

- `application.yml` 同时维护 `jwt.*`（给死代码用）和 `sa-token.*`（给实际系统用）
- `application-prod.yml` 中 `sa-token.jwt-secret-key` 硬编码，未使用环境变量

### 1.6 `WebMvcAutoConfiguration` 的 `@ComponentScan` 副作用

`@ComponentScan(basePackages = "pvt.mktech.petcare.common.web")` 扫描到 `JwtAuthInterceptor` 和 `UserInfoInterceptor`，虽未注册到拦截器链，但无谓地创建了 Bean。

---

## 二、重构方案

### Step 1：删除 Legacy JWT 死代码

#### 1a. 删除文件

| # | 文件路径 | 说明 |
|---|---------|------|
| 1 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtUtil.java` | Legacy JWT 工具类 |
| 2 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtAutoConfiguration.java` | Legacy JWT 自动配置 |
| 3 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/JwtAuthInterceptor.java` | Legacy JWT 拦截器 |
| 4 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserInfoInterceptor.java` | Legacy 用户信息拦截器 |

#### 1b. 修改文件

| # | 文件路径 | 修改内容 |
|---|---------|---------|
| 5 | `modules/pet-care-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 删除 `pvt.mktech.petcare.common.jwt.JwtAutoConfiguration` 这一行 |
| 6 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/WebMvcAutoConfiguration.java` | 删除 `@ComponentScan(basePackages = "pvt.mktech.petcare.common.web")` 注解 |
| 7 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/constant/CommonConstant.java` | 删除 JWT 相关常量：`TOKEN_HEADER`、`TOKEN_PREFIX`、`HEADER_USER_ID` |

#### 1c. 清理配置文件中的 `jwt.*` 配置段

| # | 文件路径 | 删除内容 |
|---|---------|---------|
| 8 | `modules/pet-care-core/src/main/resources/application.yml` | 删除第 29-33 行 `jwt:` 整个配置块 |
| 9 | `modules/pet-care-core/src/main/resources/application-local.yml` | 删除第 67-69 行 `jwt:` 整个配置块 |
| 10 | `modules/pet-care-core/src/main/resources/application-prod.yml` | 删除第 64-66 行 `jwt:` 整个配置块 |
| 11 | `modules/pet-care-ai/src/main/resources/application.yml` | 删除第 133-138 行 `jwt:` 整个配置块 |
| 12 | `modules/pet-care-ai/src/main/resources/application-local.yml` | 删除第 120-122 行 `jwt:` 整个配置块 |
| 13 | `modules/pet-care-ai/src/main/resources/application-prod.yml` | 删除第 116-118 行 `jwt:` 整个配置块 |

#### 1d. 清理 AI 模块无效配置

| # | 文件路径 | 删除内容 |
|---|---------|---------|
| 14 | `modules/pet-care-ai/src/main/resources/application.yml` | 删除第 140-146 行 `web.mvc.jwt-include-paths` 整个配置块 |

#### 1e. 移除 jjwt Maven 依赖

| # | 文件路径 | 修改内容 |
|---|---------|---------|
| 15 | `modules/pet-care-common/pom.xml` | 删除 `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 三个依赖（第 48-62 行） |
| 16 | `pom.xml` (parent) | 删除 `<jsonwebtoken.version>` 属性（第 31 行），删除 `dependencyManagement` 中 `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 三个依赖声明（第 152-169 行） |

> **注意**：移除 jjwt 前需全局搜索确认无其他业务代码引用 `io.jsonwebtoken` 包。

---

### Step 2：修复 `GlobalExceptionHandler`

| # | 文件路径 | 修改内容 |
|---|---------|---------|
| 17 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/exception/GlobalExceptionHandler.java` | 新增 `@ExceptionHandler(NotLoginException.class)` 方法 |

在 `handleBusinessException` 方法之后新增：

```java
@ExceptionHandler(NotLoginException.class)
public Result<String> handleNotLoginException(NotLoginException ex) {
    log.warn("未登录访问: {}", ex.getMessage());
    return Result.error(ErrorCode.UNAUTHORIZED.getCode(), "未登录或登录已过期");
}
```

需要新增 import：`cn.dev33.satoken.exception.NotLoginException`

---

### Step 3：简化 `UserContext`

| # | 文件路径 | 修改内容 |
|---|---------|---------|
| 18 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserContext.java` | 移除 Sa-Token fallback，增加 `getRequiredUserId()` |

目标代码：

```java
package pvt.mktech.petcare.common.web;

import pvt.mktech.petcare.common.exception.BusinessException;
import pvt.mktech.petcare.common.exception.ErrorCode;

public class UserContext {

    private static final ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        threadLocal.set(userId);
    }

    public static Long getUserId() {
        return threadLocal.get();
    }

    public static Long getRequiredUserId() {
        Long userId = threadLocal.get();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void removeUserId() {
        threadLocal.remove();
    }
}
```

变更说明：
- 移除 `StpUtil` 依赖和 fallback 逻辑
- 移除 `USER_INFO_KEY` 常量（未使用）
- 新增 `getRequiredUserId()` 方法，null 时直接抛 `UNAUTHORIZED`
- 移除 import `cn.dev33.satoken.stp.StpUtil`

下游影响：此修改移除了 fallback，意味着如果拦截器没有正确设置 ThreadLocal，`getUserId()` 会返回 null 而非静默从 Sa-Token 获取。这是预期行为——让问题暴露而非掩盖。

---

### Step 4：AI 模块引入独立 Sa-Token JWT 认证

#### 4a. 添加 Maven 依赖

| # | 文件路径 | 修改内容 |
|---|---------|---------|
| 19 | `modules/pet-care-ai/pom.xml` | 新增 `sa-token-spring-boot3-starter` 和 `sa-token-jwt` 依赖 |

新增内容：

```xml
<!-- SA-Token 权限认证 -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
</dependency>
<!-- SA-Token 整合 JWT -->
<dependency>
    <groupId>cn.dev33</groupId>
    <artifactId>sa-token-jwt</artifactId>
</dependency>
```

#### 4b. 新建 SaTokenConfig

| # | 文件路径 | 操作 |
|---|---------|------|
| 20 | `modules/pet-care-ai/src/main/java/pvt/mktech/petcare/infrastructure/config/SaTokenConfig.java` | **新建文件** |

```java
package pvt.mktech.petcare.infrastructure.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pvt.mktech.petcare.common.web.UserContext;

@Slf4j
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @PostConstruct
    public void init() {
        log.info("AI 模块 SA-Token JWT 模式配置初始化完成");
    }

    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();
            Long userId = StpUtil.getLoginIdAsLong();
            UserContext.setUserId(userId);
        }))
        .order(0)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/doc.html",
            "/favicon.ico",
            "/webjars/**",
            "/actuator/**",
            "/error"
        );
    }
}
```

> **注意**：AI 模块没有 `/auth/**` 登录接口，不需要排除该路径。AI 模块是纯资源服务，只做 token 校验，不生成 token。

#### 4c. 添加 Sa-Token 配置

| # | 文件路径 | 修改内容 |
|---|---------|---------|
| 21 | `modules/pet-care-ai/src/main/resources/application.yml` | 新增 `sa-token:` 配置块 |
| 22 | `modules/pet-care-ai/src/main/resources/application-local.yml` | 新增 `sa-token.jwt-secret-key` |
| 23 | `modules/pet-care-ai/src/main/resources/application-prod.yml` | 新增 `sa-token.jwt-secret-key` |

application.yml 新增（替换之前删除的 `jwt:` 和 `web.mvc:` 位置）：

```yaml
# SA-Token 配置（整合 JWT）
sa-token:
  token-name: Authorization
  timeout: 86400
  active-timeout: -1
  is-concurrent: true
  is-share: false
  is-log: false
  token-prefix: Bearer
  is-read-header: true
  is-read-cookie: false
  is-write-header: true
```

application-local.yml 新增：

```yaml
# Sa-Token 配置
sa-token:
  jwt-secret-key: ${JWT_SECRET_KEY}
```

application-prod.yml 新增：

```yaml
# Sa-Token 配置
sa-token:
  jwt-secret-key: ${JWT_SECRET_KEY}
```

---

### Step 5：统一 Core 模块配置

| # | 文件路径 | 修改内容 |
|---|---------|---------|
| 24 | `modules/pet-care-core/src/main/resources/application-prod.yml` | 将硬编码的 `sa-token.jwt-secret-key` 改为环境变量引用 |

变更（第 69 行）：

```yaml
# 变更前（原硬编码值已替换为环境变量占位）
sa-token:
  jwt-secret-key: ${JWT_SECRET_KEY}

# 变更后
sa-token:
  jwt-secret-key: ${JWT_SECRET_KEY}
```

---

## 三、涉及文件完整清单

### 需要删除的文件（4 个）

| # | 文件路径 | 模块 |
|---|---------|------|
| 1 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtUtil.java` | common |
| 2 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtAutoConfiguration.java` | common |
| 3 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/JwtAuthInterceptor.java` | common |
| 4 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserInfoInterceptor.java` | common |

### 需要新建的文件（1 个）

| # | 文件路径 | 模块 |
|---|---------|------|
| 5 | `modules/pet-care-ai/src/main/java/pvt/mktech/petcare/infrastructure/config/SaTokenConfig.java` | ai |

### 需要修改的文件（16 个）

| # | 文件路径 | 修改内容摘要 |
|---|---------|-------------|
| 6 | `pom.xml` (parent) | 删除 `jsonwebtoken.version` 属性，删除 jjwt 三件套 dependencyManagement 声明 |
| 7 | `modules/pet-care-common/pom.xml` | 删除 jjwt 三件套依赖 |
| 8 | `modules/pet-care-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 删除 `JwtAutoConfiguration` 行 |
| 9 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/WebMvcAutoConfiguration.java` | 删除 `@ComponentScan` 注解 |
| 10 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/constant/CommonConstant.java` | 删除 `TOKEN_HEADER`、`TOKEN_PREFIX`、`HEADER_USER_ID` 常量 |
| 11 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserContext.java` | 移除 Sa-Token fallback，新增 `getRequiredUserId()` |
| 12 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/exception/GlobalExceptionHandler.java` | 新增 `NotLoginException` 处理器 |
| 13 | `modules/pet-care-core/src/main/resources/application.yml` | 删除 `jwt:` 配置块 |
| 14 | `modules/pet-care-core/src/main/resources/application-local.yml` | 删除 `jwt:` 配置块 |
| 15 | `modules/pet-care-core/src/main/resources/application-prod.yml` | 删除 `jwt:` 配置块，`sa-token.jwt-secret-key` 改为 `${JWT_SECRET_KEY}` |
| 16 | `modules/pet-care-ai/pom.xml` | 新增 `sa-token-spring-boot3-starter` 和 `sa-token-jwt` 依赖 |
| 17 | `modules/pet-care-ai/src/main/resources/application.yml` | 删除 `jwt:` 和 `web.mvc.jwt-include-paths`，新增 `sa-token:` 配置块 |
| 18 | `modules/pet-care-ai/src/main/resources/application-local.yml` | 删除 `jwt:`，新增 `sa-token.jwt-secret-key` |
| 19 | `modules/pet-care-ai/src/main/resources/application-prod.yml` | 删除 `jwt:`，新增 `sa-token.jwt-secret-key: ${JWT_SECRET_KEY}` |

### 无需修改但需关注的文件（参考确认）

| # | 文件路径 | 关注原因 |
|---|---------|-------------|
| 20 | `modules/pet-care-core/src/main/java/pvt/mktech/petcare/config/SaTokenConfig.java` | 已生效的 Sa-Token 配置，本次不改动，但需确认与新架构一致 |
| 21 | `modules/pet-care-core/src/main/java/pvt/mktech/petcare/user/service/impl/AuthServiceImpl.java` | 登录/登出/token 刷新实现，本次不改动 |
| 22 | `modules/pet-care-core/src/main/java/pvt/mktech/petcare/user/controller/AuthController.java` | 认证接口控制器，本次不改动 |
| 23 | `modules/pet-care-core/src/main/java/pvt/mktech/petcare/user/dto/LoginInfoDto.java` | 登录信息 DTO，本次不改动 |
| 24 | `modules/pet-care-core/src/main/java/pvt/mktech/petcare/admin/security/RequireAdmin.java` | 管理员权限注解，本次不改动 |
| 25 | `modules/pet-care-core/src/main/java/pvt/mktech/petcare/admin/security/RequireAdminAspect.java` | 管理员权限切面，本次不改动 |
| 26 | `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/exception/ErrorCode.java` | 错误码枚举，已包含 `UNAUTHORIZED`/`TOKEN_EXPIRED` 等，本次不改动 |

---

## 四、执行顺序与依赖关系

```
Step 1 (删除死代码) ──┐
                       ├── Step 3 (简化 UserContext，依赖 Step 1 的 Sa-Token 移除)
Step 2 (修复异常处理) ──┘

Step 4 (AI 模块认证) ── 独立，可与 Step 1-3 并行

Step 5 (统一配置) ── 依赖 Step 1 完成（确认 jwt.* 已全部清理）
```

推荐执行顺序：Step 1 → Step 2 → Step 3 → Step 5 → Step 4

---

## 五、验证清单

- [ ] 全局搜索 `io.jsonwebtoken` 确认无遗漏引用
- [ ] 全局搜索 `JwtUtil`、`JwtAuthInterceptor`、`UserInfoInterceptor` 确认无遗漏引用
- [ ] 全局搜索 `TOKEN_HEADER`、`TOKEN_PREFIX`、`HEADER_USER_ID` 确认无遗漏引用
- [ ] Core 模块启动正常，登录/登出/token 刷新接口可用
- [ ] Core 模块受保护接口未携带 token 返回 401（而非 500）
- [ ] AI 模块启动正常，受保护接口未携带 token 返回 401
- [ ] AI 模块携带有效 JWT token 可正常访问，`UserContext.getUserId()` 正确返回
- [ ] `mvn clean install -DskipTests` 通过
- [ ] `mvn test` 通过
- [ ] 检查 common 模块删除 jjwt 后 AI 模块编译是否正常（AI 通过 common 传递依赖）
