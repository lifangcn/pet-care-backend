# SSE 推送通知后端实现需求

## 1. SSE 端点实现

### 1.1 端点配置
- **路径**: `/sse/notifications`
- **方法**: GET
- **认证**: 通过 Query 参数 `token` 传递 JWT token

### 1.2 请求格式
```
GET /sse/notifications?token={jwt_token}
Accept: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

**注意**: 由于浏览器 EventSource API 不支持自定义请求头，因此必须通过 URL 参数传递 token。建议：
- 生产环境使用 HTTPS 确保传输安全
- Token 有效期设置合理
- 考虑使用短期有效的 SSE token 替代长期有效的 access token

### 1.3 响应要求
- **Content-Type**: `text/event-stream`
- **字符编码**: UTF-8
- **连接保持**: 长连接，客户端不断开则保持连接
- **心跳**: 每 30 秒发送一次注释行（`: heartbeat`）保持连接活跃

## 2. 消息格式规范

### 2.1 提醒通知事件
```
event: reminder
data: {"id":"123","reminderId":"456","petId":"789","petName":"Milo","title":"疫苗接种提醒","description":"请为 Milo 接种狂犬疫苗","scheduleTime":"2024-01-15T10:00:00Z","notificationTime":"2024-01-15T09:00:00Z","type":"REMINDER"}

```
字段说明：
- `id`: 通知消息ID（字符串或数字）
- `reminderId`: 提醒ID（字符串或数字）
- `petId`: 宠物ID（字符串或数字）
- `petName`: 宠物名称（字符串）
- `title`: 通知标题（字符串）
- `description`: 通知描述（字符串）
- `scheduleTime`: 计划时间（ISO 8601 格式字符串）
- `notificationTime`: 通知时间（ISO 8601 格式字符串）
- `type`: 固定值 "REMINDER"（字符串）

### 2.2 通用消息事件
```
event: message
data: {"type":"system","title":"系统通知","content":"您的账户已更新","id":"msg_001"}

```

### 2.3 心跳保持
```
: heartbeat

```

## 3. 消息持久化

### 3.1 存储方案选择

#### 方案一：纯数据库存储（推荐用于小到中型系统）
**优点**：
- 数据持久化，不会丢失
- 支持复杂查询（按类型、时间、已读状态等）
- 适合消息中心的历史消息查询
- 数据安全可靠

**缺点**：
- 高频写入可能成为瓶颈
- 需要定期清理旧数据避免表过大

#### 方案二：Redis + 数据库混合存储（推荐用于中大型系统）
**优点**：
- Redis 用于实时推送队列，性能高
- 数据库用于持久化，保证数据不丢失
- 兼顾性能和可靠性

**缺点**：
- 架构复杂度增加
- 需要维护数据一致性

#### 方案三：纯 Redis 存储（不推荐）
**优点**：
- 性能最高
- 支持过期时间自动清理

**缺点**：
- **数据可能丢失**（Redis 持久化不是 100% 可靠）
- **无法保证消息中心能查询到历史消息**
- 内存成本高
- 不适合长期存储

**建议**：优先使用**方案二（Redis + 数据库混合）**，如果系统规模较小，使用**方案一（纯数据库）**也可以。

### 3.2 混合存储方案实现

#### Redis 存储结构（用于推送队列）
```redis
# 用户未读消息列表（Sorted Set，按时间排序）
key: notification:unread:{userId}
score: timestamp
value: messageId

# 消息内容（Hash，临时存储，过期时间 7 天）
key: notification:msg:{messageId}
field: type, title, content, reminderId, petId, petName, createdAt
expire: 7 days
```

**Redis 作用**：
- 快速查询用户未读消息数量
- 临时缓存最新消息内容
- 支持消息过期自动清理

#### 数据库表结构（用于持久化）
```sql
CREATE TABLE message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  type VARCHAR(20) NOT NULL COMMENT '消息类型: system, reminder',
  title VARCHAR(200) NOT NULL COMMENT '消息标题',
  content TEXT NOT NULL COMMENT '消息内容',
  read_status TINYINT DEFAULT 0 COMMENT '已读状态: 0-未读, 1-已读',
  reminder_id BIGINT COMMENT '提醒ID（如果是提醒类型）',
  pet_id BIGINT COMMENT '宠物ID（如果是提醒类型）',
  pet_name VARCHAR(100) COMMENT '宠物名称',
  link VARCHAR(500) COMMENT '跳转链接',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_id (user_id),
  INDEX idx_user_type (user_id, type),
  INDEX idx_read_status (user_id, read_status),
  INDEX idx_created_at (created_at)
);

-- 可选：定期清理 90 天前的已读消息
-- DELETE FROM message WHERE read_status = 1 AND created_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

**数据库作用**：
- 持久化所有消息，保证数据不丢失
- 支持消息中心的历史查询和分页
- 支持复杂条件查询（按类型、时间范围等）

### 3.3 消息存储流程（混合方案）

```java
public void sendNotification(Long userId, NotificationMessage message) {
    // 1. 保存到数据库（持久化）
    Message dbMessage = new Message();
    dbMessage.setUserId(userId);
    dbMessage.setType(message.getType());
    dbMessage.setTitle(message.getTitle());
    dbMessage.setContent(message.getContent());
    // ... 设置其他字段
    Message savedMessage = messageRepository.save(dbMessage);
    
    // 2. 写入 Redis（用于实时查询和推送）
    String unreadKey = "notification:unread:" + userId;
    String msgKey = "notification:msg:" + savedMessage.getId();
    
    // 添加到未读列表
    redisTemplate.opsForZSet().add(unreadKey, savedMessage.getId().toString(), 
        System.currentTimeMillis());
    
    // 缓存消息内容（7 天过期）
    Map<String, Object> msgData = convertToMap(savedMessage);
    redisTemplate.opsForHash().putAll(msgKey, msgData);
    redisTemplate.expire(msgKey, 7, TimeUnit.DAYS);
    
    // 3. 通过 SSE 推送给在线用户
    List<SseEmitter> emitters = sseConnectionManager.getUserConnections(userId);
    for (SseEmitter emitter : emitters) {
        try {
            emitter.send(SseEmitter.event()
                .name("reminder")
                .data(convertToJson(savedMessage)));
        } catch (Exception e) {
            sseConnectionManager.removeConnection(userId, emitter);
        }
    }
}
```

### 3.4 消息查询流程

**未读数量查询**（使用 Redis）：
```java
public int getUnreadCount(Long userId) {
    String key = "notification:unread:" + userId;
    return redisTemplate.opsForZSet().count(key).intValue();
}
```

**消息列表查询**（使用数据库）：
```java
public Page<Message> getMessages(Long userId, String type, int page, int pageSize) {
    // 从数据库查询，支持分页和条件筛选
    return messageRepository.findByUserIdAndType(userId, type, 
        PageRequest.of(page - 1, pageSize));
}
```

### 3.5 标记已读同步

```java
public void markAsRead(Long userId, Long messageId) {
    // 1. 更新数据库
    messageRepository.updateReadStatus(messageId, 1);
    
    // 2. 从 Redis 未读列表移除
    String unreadKey = "notification:unread:" + userId;
    redisTemplate.opsForZSet().remove(unreadKey, messageId.toString());
}
```

## 4. 认证与授权

### 4.1 Token 验证
- 从 Query 参数中获取 `token`
- 验证 JWT token 的有效性
- 从 token 中提取用户ID

### 4.2 连接管理
- 维护用户ID与 SSE 连接的映射关系（使用线程安全的 Map）
- 支持同一用户的多个连接（如多标签页）
- 连接断开时清理映射关系

## 5. 消息推送流程

### 5.1 提醒通知推送
1. 定时任务检测到需要推送的提醒
2. 创建消息记录并保存到数据库
3. 查找该用户的所有活跃 SSE 连接
4. 向每个连接发送提醒通知事件
5. 如果用户没有活跃连接，消息仍然保存在数据库中，用户登录后可在消息中心查看

### 5.2 推送逻辑示例（混合存储方案）
```java
// 1. 保存消息到数据库（持久化）
Message message = new Message();
message.setUserId(userId);
message.setType("reminder");
message.setTitle(reminder.getTitle());
message.setContent(reminder.getDescription());
message.setReminderId(reminder.getId());
message.setPetId(pet.getId());
message.setPetName(pet.getName());
message.setReadStatus(0);
Message savedMessage = messageRepository.save(message);

// 2. 写入 Redis（用于快速查询未读数）
String unreadKey = "notification:unread:" + userId;
redisTemplate.opsForZSet().add(unreadKey, savedMessage.getId().toString(), 
    System.currentTimeMillis());
String msgKey = "notification:msg:" + savedMessage.getId();
redisTemplate.opsForHash().putAll(msgKey, convertToMap(savedMessage));
redisTemplate.expire(msgKey, 7, TimeUnit.DAYS);

// 3. 查找用户的活跃连接并推送
List<SseEmitter> emitters = sseConnectionManager.getUserConnections(userId);
for (SseEmitter emitter : emitters) {
    try {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
            .name("reminder")
            .data(messageToJson(savedMessage));
        emitter.send(event);
    } catch (Exception e) {
        // 连接已断开，移除该连接
        sseConnectionManager.removeConnection(userId, emitter);
    }
}
```

## 6. API 接口需求

### 6.1 获取消息列表
```
GET /messages?type={type}&page={page}&pageSize={pageSize}
Authorization: Bearer {token}
```

**响应格式**:
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": "123",
        "type": "reminder",
        "title": "疫苗接种提醒",
        "content": "请为 Milo 接种狂犬疫苗",
        "read": false,
        "createdAt": "2024-01-15T09:00:00Z",
        "reminderId": "456",
        "petId": "789",
        "petName": "Milo"
      }
    ],
    "pageNumber": 1,
    "pageSize": 10,
    "totalPage": 5,
    "totalRow": 50
  }
}
```

### 6.2 标记消息已读
```
PUT /messages/{id}/read
Authorization: Bearer {token}
```

### 6.3 全部标记已读
```
PUT /messages/read-all
Authorization: Bearer {token}
```

### 6.4 删除消息
```
DELETE /messages/{id}
Authorization: Bearer {token}
```

## 7. Spring Boot 实现要点

### 7.1 依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

### 7.2 SSE Controller 示例
```java
@RestController
@RequestMapping("/sse")
public class NotificationSSEController {
    
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @RequestParam String token,
            HttpServletResponse response) {
        // 1. 验证 token 并获取用户ID
        Long userId = validateTokenAndGetUserId(token);
        
        // 2. 设置响应头
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        
        // 3. 创建 SSE Emitter（超时时间建议 24 小时）
        SseEmitter emitter = new SseEmitter(24 * 60 * 60 * 1000L);
        
        // 4. 注册连接
        sseConnectionManager.addConnection(userId, emitter);
        
        // 5. 设置完成和超时回调
        emitter.onCompletion(() -> {
            sseConnectionManager.removeConnection(userId, emitter);
        });
        emitter.onTimeout(() -> {
            sseConnectionManager.removeConnection(userId, emitter);
        });
        emitter.onError((ex) -> {
            sseConnectionManager.removeConnection(userId, emitter);
        });
        
        // 6. 启动心跳线程
        startHeartbeat(emitter);
        
        return emitter;
    }
    
    private void startHeartbeat(SseEmitter emitter) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                executor.shutdown();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
}
```

### 7.3 连接管理器
```java
@Component
public class SseConnectionManager {
    private final Map<Long, List<SseEmitter>> userConnections = new ConcurrentHashMap<>();
    
    public void addConnection(Long userId, SseEmitter emitter) {
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    }
    
    public void removeConnection(Long userId, SseEmitter emitter) {
        List<SseEmitter> connections = userConnections.get(userId);
        if (connections != null) {
            connections.remove(emitter);
            if (connections.isEmpty()) {
                userConnections.remove(userId);
            }
        }
    }
    
    public List<SseEmitter> getUserConnections(Long userId) {
        return userConnections.getOrDefault(userId, Collections.emptyList());
    }
}
```

## 8. Nginx 配置（如使用）

如果使用 Nginx 作为反向代理，需要配置：

```nginx
location /sse/notifications {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Connection '';
    proxy_set_header Cache-Control 'no-cache';
    proxy_set_header X-Accel-Buffering 'no';
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 86400s;
    proxy_send_timeout 86400s;
}
```

关键配置说明：
- `proxy_http_version 1.1`: 使用 HTTP/1.1
- `proxy_set_header Connection ''`: 清空 Connection 头
- `proxy_set_header X-Accel-Buffering 'no'`: 禁用 Nginx 缓冲
- `proxy_buffering off`: 关闭代理缓冲
- `proxy_cache off`: 关闭缓存
- `proxy_read_timeout` 和 `proxy_send_timeout`: 设置较长的超时时间（24小时）

## 9. 错误处理

### 9.1 连接错误
- Token 无效：立即关闭连接，返回 HTTP 401
- 用户不存在：立即关闭连接，返回 HTTP 403
- 连接超时：自动清理连接

### 9.2 推送错误
- 如果推送失败（连接已断开），静默处理，不抛出异常
- 定期清理无效连接

## 10. 测试要点

1. **连接测试**: 验证可以成功建立 SSE 连接
2. **认证测试**: 验证无效 token 会被拒绝
3. **推送测试**: 验证消息可以正确推送到客户端
4. **持久化测试**: 验证消息正确保存到数据库
5. **多连接测试**: 验证同一用户的多个连接都能收到消息
6. **断开重连测试**: 验证连接断开后可以重新连接
7. **心跳测试**: 验证心跳机制正常工作
8. **消息中心测试**: 验证消息中心可以正确查询和显示历史消息

