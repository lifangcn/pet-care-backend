# 前端后续开发提示词文档

## 当前日期
2026-03-27

## 概述
本文档为后台管理模块前端开发提供接口说明和开发指导。

---

## 一、后台管理接口总览

### 1.1 通用权限说明
- 所有 `/admin/**` 接口需要 `ADMIN` 角色权限
- 使用 `@RequireAdmin` 注解进行权限校验
- 需先登录并获取有效的 SA-Token

### 1.2 接口基础路径
- 核心模块：`/api` (假设前缀)
- AI 模块：`/api/ai` (假设前缀)

---

## 二、用户管理接口

### 2.1 接口列表
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/user/list` | 查询用户列表 |
| PUT | `/admin/user/{id}/enabled` | 启用/禁用用户 |
| PUT | `/admin/user/{id}/role` | 设置用户角色 |

### 2.2 关键 DTO
```typescript
// AdminUserResponse
interface AdminUserResponse {
  id: number;
  username: string;
  nickname: string;
  email: string;
  enabled: boolean;
  isAdmin: boolean;
  createdAt: string;
}

// AdminUserEnabledUpdateRequest
interface AdminUserEnabledUpdateRequest {
  enabled: boolean;
}

// AdminUserRoleUpdateRequest
interface AdminUserRoleUpdateRequest {
  isAdmin: boolean;
}
```

---

## 三、内容审核接口

### 3.1 帖子审核接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/post/list` | 查询帖子列表（带审核状态筛选） |
| PUT | `/admin/post/{id}/audit` | 审核帖子 |

### 3.2 活动审核接口
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/activity/list` | 查询活动列表（带审核状态筛选） |
| PUT | `/admin/activity/{id}/audit` | 审核活动 |

### 3.3 审核状态枚举
```typescript
type AuditStatusOfContent = 'PENDING' | 'APPROVED' | 'REJECTED';
```

---

## 四、积分券管理接口

### 4.1 接口列表
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/points/coupon/template` | 创建积分券模板 |
| GET | `/admin/points/coupon/templates` | 查询积分券模板列表（分页） |
| PUT | `/admin/points/coupon/template/{id}` | 编辑积分券模板 |
| POST | `/admin/points/coupon/template/{id}/issue` | 批量发放积分券 |
| GET | `/admin/points/coupon/records` | 查询积分流水（分页） |

### 4.2 关键 DTO
```typescript
// PointsCouponTemplateRequest
interface PointsCouponTemplateRequest {
  name: string;
  faceValue: number;
  validDays: number;
  totalCount: number;
  perUserLimit: number;
  sourceType: 'SYSTEM' | 'ACTIVITY' | 'NEWCOMER';
  status: number; // 0-停用 1-启用
}

// PointsCouponTemplate
interface PointsCouponTemplate {
  id: number;
  name: string;
  faceValue: number;
  validDays: number;
  totalCount: number;
  issuedCount: number;
  perUserLimit: number;
  sourceType: string;
  status: number;
  createdAt: string;
  updatedAt: string;
}
```

---

## 五、AI 后台管理接口

### 5.1 知识库文档管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/ai/document/upload` | 上传文档（仅支持 Markdown） |
| GET | `/admin/ai/document/list` | 查询文档列表 |
| DELETE | `/admin/ai/document/{id}` | 删除文档 |
| POST | `/admin/ai/document/{id}/reindex` | 重新索引文档 |

### 5.2 数据同步管理
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/ai/sync/posts/full` | 全量同步 Post 数据到 ES |
| POST | `/admin/ai/sync/posts/incremental` | 增量同步 Post 数据（待实现） |
| GET | `/admin/ai/sync/index/status` | 查询索引状态 |
| POST | `/admin/ai/sync/index/rebuild` | 重建索引 |

### 5.3 关键 DTO
```typescript
// KnowledgeDocumentResponse
interface KnowledgeDocumentResponse {
  id: number;
  name: string;
  fileUrl: string;
  fileType: string;
  fileSize: number;
  processingStatus: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED';
  chunkCount: number;
  processingError: string;
  createdAt: string;
}
```

---

## 六、通用响应格式

### 6.1 成功响应
```typescript
interface Result<T> {
  code: number;
  message: string;
  data: T;
}
```

### 6.2 分页响应
```typescript
interface Page<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
```

---

## 七、前端开发建议

### 7.1 页面结构建议
```
后台管理系统
├── 用户管理
│   ├── 用户列表页
│   └── 用户编辑弹窗
├── 内容审核
│   ├── 帖子审核页
│   └── 活动审核页
├── 积分管理
│   ├── 积分券模板页
│   ├── 模板创建/编辑弹窗
│   ├── 批量发放弹窗
│   └── 积分流水页
└── AI 管理
    ├── 知识库文档页
    └── 数据同步页
```

### 7.2 交互提示
1. **审核操作**：审核时应显示内容预览，防止误操作
2. **批量发放**：发放前应确认用户列表，发放后显示成功/失败统计
3. **文档上传**：仅支持 `.md` 格式，上传后显示处理状态
4. **数据同步**：同步操作是异步的，应提示用户"任务已触发"

### 7.3 权限处理
- 登录后检查用户是否为 ADMIN 角色
- 非 ADMIN 用户隐藏后台管理入口
- 所有后台接口调用前确保 token 有效

---

## 八、API 文档访问

启动项目后访问 Knife4j 文档：
- 本地开发：`http://localhost:8080/doc.html`
- 包含完整的接口说明、请求示例、响应示例

---

## 九、数据库迁移

执行迁移脚本：`scripts/db/20260327_admin_backend.sql`

---

*文档生成时间：2026-03-27*
