# 权限控制功能设计文档

## 问题分析

### 1. 是否有必要添加权限控制？

**推荐结论：需要添加，但分阶段实现**

当前项目状态分析：
- 已用户登录体系（手机号验证码、微信登录）
- 已使用 JWT + Redis 存储 refreshToken
- **数据层面已经按 user_id 隔离（所有业务表都有 user_id 字段）**
- 但接口层面 **没有任何权限校验**，任何人只要拿到接口就能访问任意用户数据

**必要性分析：**

| 角度 | 结论 | 说明 |
|------|------|------|
| **安全性** | 必须 | 目前任何人可以通过伪造请求访问/修改任意用户数据（宠物、提醒、健康记录等） |
| **业务发展** | 需要 | 后续会有管理员功能、多人协作、社区内容审核等需求 |
| **改造成本** | 低 | 基础数据已经按 user_id 隔离，只需增加拦截器/过滤器校验 |
| **运维成本** | 低 | 使用 SA-Token 封装完善，学习成本低 |

### 2. 基于 SA-Token 复杂度如何？

**推荐方案：使用 SA-Token，复杂度低**

#### SA-Token 优势：
- **国产框架，文档完善** - 对国内开发者友好
- **轻量无依赖** - 仅 100KB 左右，不依赖任何第三方容器
- **API 简洁** - `StpUtil.getLoginId()` `StpUtil.checkLogin()` 一行代码搞定
- **功能丰富** - 登录认证、权限认证、角色认证、注销、续期、踢人下线等全支持
- **适配 Spring Boot 3** - 官方已经支持 Spring Boot 3.x + Jakarta EE

#### 复杂度评估：

| 项目 | 复杂度 | 说明 |
|------|--------|------|
| 集成依赖 | ⭐️ (低) | 只需要加一个 Maven 依赖 |
| 配置 | ⭐️ (低) | 几行配置搞定 |
| 接口改造 | ⭐⭐ (中) | 需要给所有业务接口添加权限校验，但可以通过注解批量处理 |
| 数据变更 | ⭐⭐ (中) | 需要新增角色表、权限表、用户角色关联表 |
| 学习成本 | ⭐️ (低) | API 非常简洁，半天就能上手 |

#### 对比其他方案：

| 方案 | 复杂度 | 优缺点 |
|------|--------|--------|
| **SA-Token** | 低 | 推荐，轻量、简单、功能齐全 |
| Spring Security | 高 | 配置复杂，学习曲线陡，对于这个项目过重 |
| Shiro | 中 | 停更多年，不推荐 |
| 继续自用 JWT | 中 | 需要自己写拦截器、权限逻辑，维护成本高 |

**结论：SA-Token 是当前项目最优选择，成本低、收益高**

---

## 3. 需要修改哪些内容

### 3.1 表结构变更

#### 优化方案：初期只需要 2 张表，权限表后续按需扩展

当前项目只有"普通用户"和"管理员"两种角色，不需要细粒度权限控制，因此只需要：

```sql
-- 角色表
CREATE TABLE `tb_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码（如：admin, user）',
  `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`),
  INDEX `idx_status` (`status`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE `tb_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
```

**设计说明：**
- 权限表 (`tb_permission`) 和角色权限关联表 (`tb_role_permission`) 延后添加，等需要细粒度权限控制时再扩展
- 不修改 `tb_user` 表，`password` 字段等需要账号密码登录时再添加

**优点：**
- 减少不必要的表，降低初期复杂度
- 遵循 YAGNI 原则（You Ain't Gonna Need It）
- 不影响扩展性，后续需要时再添加即可

### 3.2 需要新增的代码文件

#### common 模块：
- 无新增，SA-Token 是独立依赖，直接引入即可

#### core 模块：

**实体类：**
- `pvt.mktech.petcare.user.entity.Role` - 角色实体
- `pvt.mktech.petcare.user.entity.UserRole` - 用户角色关联实体

**Mapper：**
- `RoleMapper`
- `UserRoleMapper`

**Service：**
- `RoleService` / `RoleServiceImpl`

**Controller：**
- `RoleController` - 角色管理接口（管理员用）

**配置：**
- `SaTokenConfig` - SA-Token 配置类（开启双 Token）
- `SaTokenConfigure` - 拦截器配置（全局登录校验）

### 3.3 需要修改的现有接口

**优化方案：使用全局拦截器，默认所有接口都需要登录校验，只排除公开接口**

配置拦截器，拦截所有路径，排除公开访问的路径：

| 模块 | Controller | 需要改动 | 说明 |
|------|------------|----------|------|
| **user** | AuthController | 需要改动 | 登录成功后使用 `StpUtil.login(userId)` 替换原来的 token 生成 |
| **user** | UserController | 不需要 | 已被全局拦截 |
| **pet** | PetController | 不需要 | 已被全局拦截 |
| **health** | HealthRecordController | 不需要 | 已被全局拦截 |
| **reminder** | ReminderController | 不需要 | 已被全局拦截 |
| **social** | PostController | 不需要 | list/detail 在拦截器排除，其他需要登录自动被拦截 |
| **social** | ActivityController | 不需要 | list/detail 在拦截器排除，其他需要登录自动被拦截 |
| **social** | LabelController | 不需要 | list 在拦截器排除，其他需要登录自动被拦截 |
| **points** | PointsController | 不需要 | 已被全局拦截 |
| **ai** | ChatController | 不需要 | 已被全局拦截 |
| **ai** | ChatSessionController | 不需要 | 已被全局拦截 |
| **ai** | KnowledgeDocumentController | 需要增加 `@RequiresRole("admin")` | 需要管理员权限 |
| **ai** | DataSyncController | 需要增加 `@RequiresRole("admin")` | 需要管理员权限 |

**拦截器配置示例：**
```java
registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
    .addPathPatterns("/**")
    .excludePathPatterns(
        // 认证相关
        "/auth/**",
        // 社交模块公开接口
        "/social/post/list",
        "/social/post/detail",
        "/social/activity/list",
        "/social/activity/detail",
        "/social/label/list",
        // API 文档
        "/swagger-ui/**",
        "/v3/api-docs/**",
        "/doc.html",
        // 静态资源
        "/favicon.ico"
    );
```

**优点：**
- 不需要逐个方法加 `@RequiresLogin` 注解，节省时间
- 不会遗漏，新增接口自动受到保护，更安全
- 只需要排除少数公开接口，符合"默认安全"原则

### 3.4 需要修改的服务方法

**AuthServiceImpl 核心改动：**
- `login()` - 替换 token 生成逻辑，改用 `StpUtil.login(userId)`，SA-Token 原生支持双 Token
- `logout()` - 改用 `StpUtil.logout()`
- `refreshToken()` - SA-Token 原生支持刷新，可简化适配

**数据权限过滤优化：**
- 所有业务 Service 查询方法已经按 `user_id` 过滤
- 前端不再需要传递 userId，统一通过 `StpUtil.getLoginIdAsLong()` 获取当前登录用户 ID
- 避免前端传错 userId 导致越权访问，更加安全

### 3.5 pom.xml 依赖变更

**根 pom.xml 需要添加：**

```xml
<!-- SA-Token 权限认证 -->
<sa-token.version>1.34.0</sa-token.version>
```

**pet-care-core/pom.xml 需要添加：**

```xml
<!-- SA-Token 权限认证 -->
<dependency>
    <groupId>cn.dev32</groupId>
    <artifactId>sa-token-spring-boot3-starter</artifactId>
    <version>${sa-token.version}</version>
</dependency>
```

---

## 实施计划

### 第一阶段：基础登录认证（最低成本）
- [ ] 添加 SA-Token 依赖
- [ ] 新增数据库表（角色、用户角色）
- [ ] 配置 SA-Token（开启双 Token）
- [ ] 配置全局拦截器（默认所有接口需要登录，排除公开接口）
- [ ] 修改 AuthController + AuthServiceImpl 适配 SA-Token
- [ ] 修改业务接口获取当前用户方式（从 `StpUtil.getLoginIdAsLong()` 获取，不再从前端传）
- [ ] 初始化数据（添加 admin 角色和 user 角色）

### 第二阶段：角色权限控制（按需扩展）
- [ ] 实现角色管理接口
- [ ] 给管理员接口添加 `@RequiresRole("admin")` 注解

### 第三阶段：细粒度权限扩展（未来需要时）
- [ ] 新增权限表 `tb_permission` 和角色权限关联表 `tb_role_permission`
- [ ] 实现权限管理接口
- [ ] 给接口添加 `@RequiresPermission("xxx")` 权限校验

---

## 初始数据

```sql
-- 初始化默认角色
INSERT INTO `tb_role` (`role_code`, `role_name`, `description`, `sort`, `status`) VALUES
('admin', '超级管理员', '系统管理员，拥有所有权限', 1, 1),
('user', '普通用户', '普通注册用户，拥有基础功能权限', 2, 1);

-- 给已有用户分配默认角色
-- 假设已有用户ID 1 为管理员
INSERT INTO `tb_user_role` (`user_id`, `role_id`) VALUES
(1, 1),  -- 用户1 拥有 admin 角色
(1, 2);  -- 用户1 也拥有 user 角色
```

---

## 影响范围评估

| 维度 | 影响范围 | 说明 |
|------|----------|------|
| **数据库** | 新增 **2 张表**，不修改原有表 | 不影响现有数据 |
| **新增代码** | 约 10-15 个文件 | 都是新增，不影响原有代码结构 |
| **修改代码** | AuthController + AuthServiceImpl 适配，业务接口改获取用户方式 | 改动集中，容易测试 |
| **业务接口** | 大多数接口不需要改注解，全局拦截自动保护 | 改动量小 |
| **兼容性** | 向前兼容 | 只增加权限校验，不改变原有业务逻辑 |

---

## 优化总结

### 优化前后对比

| 项 | 原方案 | 优化方案 | 改进点 |
|----|--------|----------|--------|
| **表数量** | 新增 4 张 + 修改 1 张 | 新增 **2 张**，不修改原有表 | 减少 2 张表，遵循 YAGNI 原则 |
| **接口校验** | 每个方法加 `@RequiresLogin` | **全局拦截器**，排除公开接口 | 更安全，更少代码，不易遗漏 |
| **获取用户 ID** | 前端传参数 | `StpUtil.getLoginIdAsLong()` 后端获取 | 避免越权，更安全 |
| **权限表** | 一开始就加 | 后续需要时再加 | 降低初期复杂度 |
| **password 字段** | 预留添加 | 后续需要时再加 | 不改变现有表结构 |
| **改动量** | 较大 | 中等 | 减少不必要的改动 |
| **复杂度** | 中 | 低 | 初期更简单，后续按需扩展 |

---

## 最终结论

| 问题 | 结论 |
|------|------|
| **1. 是否需要权限控制** | **需要**，现有接口无任何保护，数据安全无保障 |
| **2. SA-Token 复杂度** | **低**，集成简单，API 简洁，维护成本低，比自己维护 JWT 更可靠 |
| **3. 修改范围** | 新增 2 张表（角色、用户角色），新增实体/Service/配置类，修改登录逻辑适配 SA-Token，业务接口改为从 SA-Token 获取当前用户 ID |

优化方案已完成，确认后可以开始实施。
