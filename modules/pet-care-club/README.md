# 宠物关怀-俱乐部
### **1.内容广场模块**

> 发布图文或视频，可以是宠物好用的东西链接，比较便宜的洗澡美容服务店或者平台链接，可以是适合宠物游玩的地点分享，支持点赞和打分（简单实现）

#### **数据库表设计**

sql

```
-- 1. 动态表 (核心表)
CREATE TABLE `tb_post` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '发布者ID',
  `title` VARCHAR(200) COMMENT '标题',
  `content` TEXT COMMENT '内容描述',
  `post_type` TINYINT NOT NULL COMMENT '1-好物分享 2-服务推荐 3-地点推荐 4-日常分享',
  `media_urls` JSON COMMENT '图片/视频URL数组 [{"url":"","type":1,"thumbnail":""}]',
  `external_link` VARCHAR(500) COMMENT '外部链接（商品、服务、地图）',
  `location_info` JSON COMMENT '地点信息 {"address":"","city":"","district":""}',
  `price_range` VARCHAR(50) COMMENT '价格区间（如：100-200元）',
  `like_count` INT DEFAULT 0,
  `rating_count` INT DEFAULT 0 COMMENT '评分次数',
  `rating_total` INT DEFAULT 0 COMMENT '评分总分',
  `rating_avg` DECIMAL(3,2) DEFAULT 0 COMMENT '平均分',
  `view_count` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '1-正常 2-隐藏 3-删除',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='动态表';

-- 2. 标签表
CREATE TABLE `tb_tag` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名',
  `type` TINYINT DEFAULT 1 COMMENT '1-通用标签 2-宠物品种 3-内容类型',
  `icon` VARCHAR(200) COMMENT '标签图标',
  `color` VARCHAR(20) COMMENT '标签颜色',
  `use_count` INT DEFAULT 0,
  `is_recommended` TINYINT DEFAULT 0 COMMENT '是否推荐标签',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='标签表';

-- 3. 动态标签关联表
CREATE TABLE `tb_post_tag` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `tag_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='动态标签关联表';

-- 4. 互动表（点赞+评分）
CREATE TABLE `tb_interaction` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  `interaction_type` TINYINT NOT NULL COMMENT '1-点赞 2-评分',
  `rating_value` TINYINT COMMENT '评分值 1-5',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT='互动表（点赞/评分）';
```



#### **后端功能规划**

- **动态管理**：
    - `POST /api/posts` - 发布动态
        - 支持多图上传（MaxIO存储）
        - 支持外部链接（自动生成预览信息）
        - 自动提取地理位置信息
    - `GET /api/posts` - 动态列表
        - 支持按类型筛选
        - 支持按标签筛选
        - 支持按城市筛选
        - 排序：最新、最热、评分最高
    - `GET /api/posts/{id}` - 动态详情
    - `PUT /api/posts/{id}` - 编辑动态（仅发布者）
    - `DELETE /api/posts/{id}` - 删除动态
- **互动功能**：
    - `POST /api/posts/{id}/like` - 点赞/取消点赞
    - `POST /api/posts/{id}/rate` - 评分（1-5星）
    - `GET /api/posts/{id}/ratings` - 获取评分详情
- **标签管理**：
    - `GET /api/tags` - 标签列表（支持按类型筛选）
    - `GET /api/tags/hot` - 热门标签
    - `POST /api/tags/suggest` - 标签建议（输入联想）

#### **前端功能规划**

- **动态发布页**：
    - 类型选择：好物/服务/地点/日常
    - 内容编辑器：文本+图片上传
    - 外部链接输入（自动识别类型）
    - 地点选择（简单城市/区域选择）
    - 标签选择（输入联想+热门标签）
    - 价格区间输入
- **内容广场页**：
    - 顶部筛选栏：类型标签、城市筛选
    - 动态卡片：图片、标题、评分、标签、价格
    - 排序切换：最新、最热、评分
    - 无限滚动加载
- **动态详情页**：
    - 完整内容展示
    - 图片/视频轮播
    - 评分展示（星星评分）
    - 互动按钮（点赞、评分）
    - 标签展示
    - 外部链接跳转

### **2. 标签系统模块**

> 可以打标签进行进行分类，用户也可以通过这些标签来获取相应的内容

#### **数据库表设计**

（已包含在上方的`tb_tag`和`tb_post_tag`中）

#### **后端功能规划**

- **标签搜索与筛选**：
    - `GET /api/posts/by-tag/{tagId}` - 按标签获取动态
    - `GET /api/tags/search` - 搜索标签
    - 标签热度计算：`use_count = 最近7天使用次数`
- **标签推荐**：
    - 基于用户历史选择推荐
    - 基于相似动态推荐
    - 热门标签推荐

#### **前端功能规划**

- **标签云组件**：
    - 热门标签展示（大小代表热度）
    - 颜色区分类型
    - 点击跳转到对应标签的动态列表
- **标签筛选器**：
    - 多标签选择筛选
    - 已选标签展示
    - 清除筛选功能

### **3. 活动打卡模块**

> 活动打卡，线上分享活动、或者是线下大型集会，发布和报名

#### **数据库表设计**

sql

```
-- 5. 活动表（最后一张表）
CREATE TABLE `tb_activity` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '创建者ID',
  `title` VARCHAR(200) NOT NULL,
  `description` TEXT,
  `cover_image` VARCHAR(500),
  `activity_type` TINYINT NOT NULL COMMENT '1-线上活动 2-线下聚会',
  `activity_time` DATETIME NOT NULL,
  `end_time` DATETIME,
  `address` VARCHAR(500) COMMENT '线下地址',
  `online_link` VARCHAR(500) COMMENT '线上链接',
  `max_participants` INT DEFAULT 0 COMMENT '0-不限',
  `current_participants` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1 COMMENT '1-招募中 2-进行中 3-已结束',
  `tags` JSON COMMENT '活动标签数组',
  `checkin_enabled` TINYINT DEFAULT 1 COMMENT '是否开启打卡',
  `checkin_count` INT DEFAULT 0 COMMENT '打卡人数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user` (`user_id`),
  INDEX `idx_time_status` (`activity_time`, `status`)
) COMMENT='活动表';

-- 活动报名和打卡共用tb_post表，通过post_type=5标识为活动打卡
-- 在tb_post表中添加字段关联活动
ALTER TABLE `tb_post` 
ADD COLUMN `activity_id` BIGINT DEFAULT NULL COMMENT '关联的活动ID',
ADD COLUMN `is_checkin` TINYINT DEFAULT 0 COMMENT '是否为活动打卡',
ADD INDEX `idx_activity` (`activity_id`);
```



#### **后端功能规划**

- **活动管理**：
    - `POST /api/activities` - 创建活动
        - 基础信息（标题、描述、时间）
        - 类型设置（线上/线下）
        - 参与人数限制
        - 是否开启打卡
    - `GET /api/activities` - 活动列表
        - 按状态筛选：即将开始、进行中、已结束
        - 按类型筛选：线上、线下
        - 按时间排序
    - `GET /api/activities/{id}` - 活动详情
    - `POST /api/activities/{id}/join` - 报名活动
    - `GET /api/activities/{id}/participants` - 参与用户列表
- **打卡功能**：
    - `POST /api/activities/{id}/checkin` - 活动打卡
        - 创建一个特殊类型的动态（post_type=5）
        - 自动关联活动ID
        - 内容包含打卡文字和图片
    - `GET /api/activities/{id}/checkins` - 获取打卡动态

#### **前端功能规划**

- **活动列表页**：
    - 活动卡片：封面图、标题、时间、参与人数、状态标签
    - 状态筛选：全部、招募中、进行中、已结束
    - 创建活动按钮
- **活动创建页**：
    - 基础信息表单
    - 时间选择器
    - 地址/链接输入
    - 开关设置：人数限制、打卡功能
- **活动详情页**：
    - 活动完整信息展示
    - 参与按钮/已参与状态
    - 参与用户列表
    - 打卡入口（已参与用户可见）
    - 打卡墙（所有打卡动态）
- **打卡发布页**：
    - 类似动态发布，但自动关联活动
    - 内容聚焦活动体验
    - 图片上传展示活动瞬间