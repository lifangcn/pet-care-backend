-- 创建数据库
CREATE DATABASE IF NOT EXISTS `pet_care_club` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `pet_care_club`;

-- 1. 动态表 (核心表)
CREATE TABLE `tb_post` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '发布者ID',
  `title` VARCHAR(200) COMMENT '标题',
  `content` TEXT COMMENT '内容描述',
  `post_type` TINYINT NOT NULL COMMENT '1-好物分享 2-服务推荐 3-地点推荐 4-日常分享 5-活动打卡',
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
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联的活动ID',
  `is_checkin` TINYINT DEFAULT 0 COMMENT '是否为活动打卡',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user` (`user_id`),
  INDEX `idx_type` (`post_type`),
  INDEX `idx_activity` (`activity_id`)
) COMMENT='动态表';

-- 2. 标签表
CREATE TABLE `tb_label` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名',
  `type` TINYINT DEFAULT 1 COMMENT '1-通用标签 2-宠物品种 3-内容类型',
  `icon` VARCHAR(200) COMMENT '标签图标',
  `color` VARCHAR(20) COMMENT '标签颜色',
  `use_count` INT DEFAULT 0,
  `is_recommended` TINYINT DEFAULT 0 COMMENT '是否推荐标签',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_type` (`type`)
) COMMENT='标签表';

-- 3. 动态标签关联表
CREATE TABLE `tb_post_label` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `post_id` BIGINT NOT NULL,
  `label_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_post` (`post_id`),
  INDEX `idx_tag` (`label_id`)
) COMMENT='动态标签关联表';

-- 4. 互动表（点赞+评分）
CREATE TABLE `tb_interaction` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `post_id` BIGINT NOT NULL,
  `interaction_type` TINYINT NOT NULL COMMENT '1-点赞 2-评分',
  `rating_value` TINYINT COMMENT '评分值 1-5',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_post_type` (`user_id`, `post_id`, `interaction_type`),
  INDEX `idx_post` (`post_id`)
) COMMENT='互动表（点赞/评分）';

-- 5. 活动表
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
  `labels` JSON COMMENT '活动标签数组',
  `checkin_enabled` TINYINT DEFAULT 1 COMMENT '是否开启打卡',
  `checkin_count` INT DEFAULT 0 COMMENT '打卡人数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user` (`user_id`),
  INDEX `idx_time_status` (`activity_time`, `status`)
) COMMENT='活动表';
