# 身份验证代码重构设计文档

## 1. 问题分析

### 1.1 当前代码混乱点

经过代码探索，发现以下问题：

| 问题 | 位置 | 影响 |
|------|------|------|
| **双系统并存** | JwtUtil + Sa-Token 同时存在 | 维护成本高，新开发者困惑 |
| **配置重复** | application.yml 和 application-local.yml 重复 JWT/Sa-Token 配置 | 容易出错，配置不一致 |
| **UserContext 职责混乱** | 同时依赖 ThreadLocal 和 Sa-Token | 违反单一职责原则 |
| **测试模式硬编码** | JwtAuthInterceptor 中 `System.getProperty("jwt.test.mode")` | 不符合配置化原则 |

### 1.2 决策依据

- **重构目标**：统一使用 Sa-Token
- **重构范围**：Core 和 Common 模块（AI 模块后续处理）
- **优先级**：代码清理优先
- **方案选择**：渐进重构（保留 JwtUtil 标记废弃，新代码统一使用 Sa-Token）

---

## 2. 设计方案

### 2.1 核心原则

- **最小改动**：只清理混乱，不改变业务逻辑
- **向后兼容**：保留 JwtUtil 但标记废弃，不影响现有功能
- **单一职责**：UserContext 只负责 ThreadLocal，认证交给 Sa-Token

### 2.2 改动范围

| 文件 | 操作 | 说明 |
|------|------|------|
| `application-local.yml` | 删除重复配置 | 统一配置源 |
| `UserContext.java` | 移除 StpUtil 依赖 | 简化职责 |
| `JwtUtil.java` | 添加 @Deprecated | 标记废弃，引导迁移 |
| `JwtAuthInterceptor.java` | 添加 @Deprecated | 标记废弃，引导迁移 |

---

## 3. 详细改动

### 3.1 删除重复配置

**文件**：`modules/pet-care-core/src/main/resources/application-local.yml`

**删除内容**：
```yaml
# JWT密钥配置
jwt:
  secret-key: ${JWT_SECRET_KEY}

# Sa-Token 配置
sa-token:
  jwt-secret-key: ${SA_TOKEN_JWT_SECRET_KEY}
```

**原因**：
- 这些配置在 `application.yml` 中已有定义
- `application-local.yml` 只应该包含环境差异配置（如数据库、Redis 连接信息）
- 统一配置源，避免配置不一致

---

### 3.2 简化 UserContext

**文件**：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserContext.java`

**改动前**：
```java
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
```

**改动后**：
```java
public static Long getUserId() {
    return threadLocal.get();
}
```

**原因**：
- UserContext 职责单一：只负责 ThreadLocal 存取
- Sa-Token 认证逻辑在 SaTokenConfig 拦截器中处理
- 避免循环依赖和职责混乱

---

### 3.3 标记 JwtUtil 为废弃

**文件**：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtUtil.java`

**改动**：
```java
/**
 * JWT 工具类
 * @deprecated 已废弃，请使用 Sa-Token 进行身份验证。
 * 新代码应使用 StpUtil.login()、StpUtil.checkLogin() 等 Sa-Token API。
 * 此类保留用于向后兼容，将在未来版本中移除。
 */
@Deprecated
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtUtil {
    // ... 原有代码不变
}
```

---

### 3.4 标记 JwtAuthInterceptor 为废弃

**文件**：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/JwtAuthInterceptor.java`

**改动**：
```java
/**
 * JWT 认证拦截器，替代 Gateway 的 JwtAuthFilter
 * @deprecated 已废弃，请使用 SaTokenConfig 中的 SaInterceptor 进行身份验证。
 * 此拦截器保留用于向后兼容，将在未来版本中移除。
 */
@Deprecated
@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements HandlerInterceptor {
    // ... 原有代码不变
}
```

---

## 4. 影响范围评估

### 4.1 受影响的文件

| 文件 | 影响类型 | 风险 |
|------|----------|------|
| `application-local.yml` | 配置删除 | 低 - 功能不变，只是统一配置源 |
| `UserContext.java` | 代码简化 | 低 - 调用方不变，行为一致 |
| `JwtUtil.java` | 添加注解 | 无 - 只是标记，功能不变 |
| `JwtAuthInterceptor.java` | 添加注解 | 无 - 只是标记，功能不变 |

### 4.2 不受影响的部分

- **业务逻辑**：所有业务代码保持不变
- **API 接口**：所有接口行为保持不变
- **Sa-Token 配置**：SaTokenConfig 保持不变
- **AI 模块**：本次不动，后续单独处理

---

## 5. 验证计划

### 5.1 功能验证

1. **登录流程**：验证手机号验证码登录正常
2. **Token 刷新**：验证 Refresh Token 刷新正常
3. **接口认证**：验证需要认证的接口正常拦截
4. **SSE 连接**：验证 SSE 场景的 Token 传递正常

### 5.2 配置验证

1. **启动日志**：确认 JWT 配置正确加载
2. **Sa-Token 日志**：确认 Sa-Token JWT 模式正常初始化

---

## 6. 后续计划

本次重构只做代码清理，后续迁移计划：

1. **新功能开发**：统一使用 Sa-Token API
2. **AI 模块迁移**：统一使用 Sa-Token 认证
3. **移除废弃代码**：确认无使用后移除 JwtUtil 和 JwtAuthInterceptor

---

## 7. 总结

| 项目 | 内容 |
|------|------|
| **改动量** | 4 个文件 |
| **预计时间** | 30 分钟 |
| **风险等级** | 低 |
| **验证方式** | 功能测试 + 启动日志检查 |
