# 后台管理模块实施计划

## 架构决策

**部署模式**：同应用部署
- 在现有应用中增加 `/admin/**` 路由
- 通过路径前缀区分用户端和管理端
- 前端管理后台部署到独立子域名

---

## 一、权限体系改造

### 1.1 新增文件

**RoleEnum 枚举**
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/constant/RoleEnum.java`
```java
public enum RoleEnum {
    USER("USER", "普通用户"),
    ADMIN("ADMIN", "管理员");
}
```

**权限注解**
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/annotation/RequireRole.java`

**角色拦截器**
- `modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/RoleCheckInterceptor.java`

### 1.2 修改文件

**JwtUtil.java** - 扩展支持角色
- 文件：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/jwt/JwtUtil.java`
- 新增方法：`generateAccessToken(Long userId, String role)`
- 新增方法：`getRoleFromToken(String token)`

**User.java** - 新增角色字段
- 文件：`modules/pet-care-core/src/main/java/pvt/mktech/petcare/user/entity/User.java`
- 新增字段：`private String role = "USER";`

**UserContext.java** - 支持角色获取
- 文件：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/UserContext.java`
- 新增方法：`setUserRole()`, `getUserRole()`

**WebMvcAutoConfiguration.java** - 注册角色拦截器
- 文件：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/WebMvcAutoConfiguration.java`
- 新增 `RoleCheckInterceptor` 注册，order=3

**JwtAuthInterceptor.java** - 解析角色
- 文件：`modules/pet-care-common/src/main/java/pvt/mktech/petcare/common/web/JwtAuthInterceptor.java`
- 将角色设置到 request attribute

### 1.3 数据库变更

```sql
ALTER TABLE tb_user ADD COLUMN role VARCHAR(20) DEFAULT 'USER' COMMENT '角色：USER/ADMIN';
ALTER TABLE tb_user ADD INDEX idx_role (role);
UPDATE tb_user SET role = 'ADMIN' WHERE id = 1; -- 初始化管理员
```

---

## 二、管理端 Controller

### 2.1 内容审核

**动态审核**
- `modules/pet-care-core/src/main/java/pvt/mktech/petcare/social/controller/admin/AdminPostController.java`
- 路径：`/admin/post`
- 接口：
  - `GET /pending` - 待审核列表
  - `PUT /{id}/approve` - 审核通过
  - `PUT /{id}/reject` - 审核拒绝

**活动审核**
- `modules/pet-care-core/src/main/java/pvt/mktech/petcare/social/controller/admin/AdminActivityController.java`
- 路径：`/admin/activity`
- 接口：
  - `GET /pending` - 待审核列表
  - `PUT /{id}/approve` - 审核通过
  - `PUT /{id}/reject` - 审核拒绝

### 2.2 积分券管理

- `modules/pet-care-core/src/main/java/pvt/mktech/petcare/points/controller/admin/AdminPointsCouponController.java`
- 路径：`/admin/points/coupon`
- 接口：
  - `POST /template` - 创建券模板
  - `GET /templates` - 模板列表
  - `PUT /template/{id}` - 编辑模板
  - `POST /template/{id}/issue` - 批量发放
  - `GET /records` - 积分流水

### 2.3 知识库文档管理

- `modules/pet-care-ai/src/main/java/pvt/mktech/petcare/knowledge/controller/admin/AdminKnowledgeDocumentController.java`
- 路径：`/admin/ai/document`
- 接口：
  - `POST /upload` - 上传文档
  - `GET /list` - 文档列表（支持分类筛选）
  - `DELETE /{id}` - 删除文档及向量
  - `POST /{id}/reindex` - 重新生成向量

### 2.4 数据同步管理

- `modules/pet-care-ai/src/main/java/pvt/mktech/petcare/sync/controller/admin/AdminDataSyncController.java`
- 路径：`/admin/ai/sync`
- 接口：
  - `POST /posts/full` - 全量同步 Post
  - `POST /posts/incremental` - 增量同步
  - `GET /index/status` - 索引状态查询
  - `POST /index/rebuild` - 重建索引

### 2.5 用户管理

- `modules/pet-care-core/src/main/java/pvt/mktech/petcare/user/controller/admin/AdminUserController.java`
- 路径：`/admin/user`
- 接口：
  - `GET /list` - 用户列表
  - `PUT /{id}/role` - 修改角色
  - `PUT /{id}/enabled` - 启用/禁用

---

## 三、配置变更

### 3.1 application.yml

```yaml
web:
  mvc:
    jwt-include-paths:
      # ... 现有路径
      - /admin/**  # 新增管理端需要认证
```

---

## 四、前端修改

### 4.1 动态编辑功能修复

- `src/views/club/PostPublish.vue` - 支持编辑模式（根据 id 参数判断）
- `src/views/club/Posts.vue` - 添加编辑按钮（仅作者可见）

### 4.2 新增管理后台页面

建议在 `src/views/admin/` 下创建：
- `PostAudit.vue` - 动态审核
- `ActivityAudit.vue` - 活动审核
- `CouponManage.vue` - 券模板管理
- `DocumentManage.vue` - 知识库文档管理
- `SyncManage.vue` - 数据同步管理
- `UserManage.vue` - 用户管理

---

## 五、实施顺序

### 阶段一：权限体系（优先）
1. 创建 RoleEnum、@RequireRole、RoleCheckInterceptor
2. User 实体添加 role 字段，执行数据库变更
3. 扩展 JwtUtil、UserContext
4. 更新 WebMvcAutoConfiguration
5. 测试权限拦截

### 阶段二：内容审核
1. 创建 AdminPostController、AdminActivityController
2. 实现审核接口
3. 前端对接

### 阶段三：积分券管理
1. 创建 AdminPointsCouponController
2. 实现模板 CRUD
3. 前端对接

### 阶段四：知识库与同步
1. 创建 AdminKnowledgeDocumentController
2. 创建 AdminDataSyncController
3. 前端对接

### 阶段五：用户管理
1. 创建 AdminUserController
2. 前端对接

---

## 六、关键文件清单

### 新增文件
| 文件路径 |
|---------|
| `modules/pet-care-common/.../RoleEnum.java` |
| `modules/pet-care-common/.../RequireRole.java` |
| `modules/pet-care-common/.../RoleCheckInterceptor.java` |
| `modules/pet-care-core/.../admin/AdminPostController.java` |
| `modules/pet-care-core/.../admin/AdminActivityController.java` |
| `modules/pet-care-core/.../admin/AdminPointsCouponController.java` |
| `modules/pet-care-core/.../admin/AdminUserController.java` |
| `modules/pet-care-ai/.../admin/AdminKnowledgeDocumentController.java` |
| `modules/pet-care-ai/.../admin/AdminDataSyncController.java` |

### 修改文件
| 文件路径 | 修改内容 |
|---------|---------|
| `modules/pet-care-common/.../jwt/JwtUtil.java` | 扩展角色支持 |
| `modules/pet-care-common/.../web/UserContext.java` | 新增角色获取 |
| `modules/pet-care-common/.../web/JwtAuthInterceptor.java` | 解析角色 |
| `modules/pet-care-common/.../web/WebMvcAutoConfiguration.java` | 注册角色拦截器 |
| `modules/pet-care-core/.../user/entity/User.java` | 新增 role 字段 |
| `modules/pet-care-core/.../resources/application.yml` | 添加 /admin/** 路径 |
| `src/views/club/PostPublish.vue` | 支持编辑模式 |

---

## 七、下次会话执行指令

**启动指令**：
```
请阅读 /Users/michael/IdeaProjects/petcare/docs/admin-backend-plan.md
按照计划中的"阶段一：权限体系"开始实施
```

**关键上下文**：
- 后端项目：`/Users/michael/IdeaProjects/petcare`
- 前端项目：`/Users/michael/VueProjects/pet-care-vue/`
- 架构模式：同应用部署（`/admin/**` 路由）
- 技术栈：Spring Boot 3.3 + MyBatis-Flex + Vue 3 + Element Plus
