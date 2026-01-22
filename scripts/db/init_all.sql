-- {@code description}: 宠物关怀系统 - 完整建表脚本
-- {@code date}: 2025-01-22
-- {@code author}: Michael
--
-- 说明：
-- 1. 包含所有模块的表（core、social、ai）
-- 2. 所有表统一使用 is_deleted 和 deleted_at 字段进行逻辑删除
-- 3. status 字段仅用于业务状态，与删除状态分离
-- 4. 使用方式：直接执行此脚本重建所有数据库

-- =============================================================================
-- 创建数据库
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `pet_care_core` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `pet_care_ai` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =============================================================================
-- Core 数据库（用户、宠物、健康、提醒、社交）
-- =============================================================================
USE `pet_care_core`;

-- -----------------------------------------------------------------------------
-- 1. 用户相关表
-- -----------------------------------------------------------------------------

-- 用户表
CREATE TABLE `tb_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '用户头像URL',
  `status` TINYINT DEFAULT 1 COMMENT '用户状态：1-正常，0-禁用',
  `address` VARCHAR(200) DEFAULT NULL COMMENT '用户地址',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- -----------------------------------------------------------------------------
-- 2. 宠物相关表
-- -----------------------------------------------------------------------------

-- 宠物表
CREATE TABLE `tb_pet` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '宠物名字',
  `type` VARCHAR(20) DEFAULT NULL COMMENT '宠物类型',
  `breed` VARCHAR(50) DEFAULT NULL COMMENT '宠物品种',
  `gender` TINYINT DEFAULT NULL COMMENT '性别：1-公，0-母',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `weight` DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '宠物头像URL',
  `health_notes` VARCHAR(500) DEFAULT NULL COMMENT '健康备注',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='宠物表';

-- 健康记录表
CREATE TABLE `tb_health_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `record_type` VARCHAR(20) NOT NULL COMMENT '记录类型: weight(体重), temperature(体温), medical(用药)',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
  `description` TEXT COMMENT '描述',
  `record_time` DATETIME NOT NULL COMMENT '记录时间',
  `value` DECIMAL(10,2) DEFAULT NULL COMMENT '数值(体重/体温等)',
  `symptom` VARCHAR(500) DEFAULT NULL COMMENT '症状信息',
  `medication_info` VARCHAR(500) DEFAULT NULL COMMENT '用药信息',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  INDEX `idx_pet_id` (`pet_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='健康记录表';

-- -----------------------------------------------------------------------------
-- 3. 提醒相关表
-- -----------------------------------------------------------------------------

-- 提醒事件表
CREATE TABLE `tb_reminder` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `source_type` VARCHAR(20) NOT NULL COMMENT '记录来源：manual, health_record, system',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源ID（如健康记录ID）',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
  `description` TEXT COMMENT '描述',
  `record_time` DATETIME NOT NULL COMMENT '记录时间',
  `schedule_time` DATETIME DEFAULT NULL COMMENT '计划时间(用于提醒)',
  `remind_before_minutes` INT DEFAULT 0 COMMENT '提前提醒时间(分钟)',
  `repeat_type` VARCHAR(20) DEFAULT 'none' COMMENT '重复类型: none(不重复), daily(每天), weekly(每周), monthly(每月), custom(自定义)',
  `repeat_config` JSON DEFAULT NULL COMMENT '重复配置(自定义重复规则)',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否激活',
  `total_occurrences` INT DEFAULT 0 COMMENT '总执行次数',
  `completed_count` INT DEFAULT 0 COMMENT '已完成次数',
  `completed_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `next_trigger_time` DATETIME DEFAULT NULL COMMENT '下一次触发时间（内部调度使用，用户不可见）',
  `reminder_execution_id` BIGINT DEFAULT NULL COMMENT '提醒执行记录ID，当前提醒和执行记录关联，标识最新的执行情况',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  INDEX `idx_pet_id` (`pet_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_schedule_time` (`schedule_time`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='提醒事件表';

-- 提醒执行记录表
CREATE TABLE `tb_reminder_execution` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `reminder_id` BIGINT NOT NULL COMMENT '提醒ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `schedule_time` DATETIME NOT NULL COMMENT '计划执行时间',
  `actual_time` DATETIME DEFAULT NULL COMMENT '实际执行时间',
  `status` ENUM('pending','completed','overdue') DEFAULT 'pending' COMMENT '执行状态',
  `completion_notes` TEXT COMMENT '完成说明',
  `notification_time` DATETIME NOT NULL COMMENT '通知时间',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '是否已读',
  `is_sent` TINYINT(1) DEFAULT 0 COMMENT '是否已发送',
  `sent_at` DATETIME DEFAULT NULL COMMENT '发送时间',
  `read_at` DATETIME DEFAULT NULL COMMENT '阅读时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  INDEX `idx_reminder_id` (`reminder_id`),
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_schedule_time` (`schedule_time`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='提醒执行记录表';

-- -----------------------------------------------------------------------------
-- 4. 社交相关表
-- -----------------------------------------------------------------------------

-- 动态表
CREATE TABLE `tb_post` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '动态ID',
  `user_id` BIGINT NOT NULL COMMENT '发布者ID',
  `title` VARCHAR(200) COMMENT '标题',
  `content` TEXT COMMENT '内容描述',
  `post_type` TINYINT NOT NULL COMMENT '1-好物分享 2-服务推荐 3-地点推荐 4-日常分享 5-活动打卡',
  `media_urls` JSON COMMENT '图片/视频URL数组 [{"url":"","type":1,"thumbnail":""}]',
  `external_link` VARCHAR(500) COMMENT '外部链接（商品、服务、地图）',
  `location_info` JSON COMMENT '地点信息 {"address":"","city":"","district":""}',
  `price_range` VARCHAR(50) COMMENT '价格区间（如：100-200元）',
  `like_count` INT DEFAULT 0 COMMENT '点赞数',
  `rating_count` INT DEFAULT 0 COMMENT '评分次数',
  `rating_total` INT DEFAULT 0 COMMENT '评分总分',
  `rating_avg` DECIMAL(3,2) DEFAULT 0 COMMENT '平均分',
  `view_count` INT DEFAULT 0 COMMENT '浏览数',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，2-隐藏',
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联的活动ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_activity_id` (`activity_id`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态表';

-- 标签表
CREATE TABLE `tb_label` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '标签ID',
  `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名',
  `type` TINYINT DEFAULT 1 COMMENT '1-通用标签 2-宠物品种 3-内容类型',
  `icon` VARCHAR(200) COMMENT '标签图标',
  `color` VARCHAR(20) COMMENT '标签颜色',
  `use_count` INT DEFAULT 0 COMMENT '使用次数',
  `is_recommended` TINYINT DEFAULT 0 COMMENT '是否推荐标签',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  INDEX `idx_type` (`type`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 动态标签关联表
CREATE TABLE `tb_post_label` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '关联ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `label_id` BIGINT NOT NULL COMMENT '标签ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  UNIQUE KEY `uk_post_label` (`post_id`, `label_id`),
  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_label_id` (`label_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='动态标签关联表';

-- 互动表（点赞+评分）
CREATE TABLE `tb_interaction` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '互动ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `post_id` BIGINT NOT NULL COMMENT '动态ID',
  `interaction_type` TINYINT NOT NULL COMMENT '1-点赞 2-评分',
  `rating_value` TINYINT COMMENT '评分值 1-5',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_user_post_type` (`user_id`, `post_id`, `interaction_type`),
  INDEX `idx_post_id` (`post_id`),
  INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互动表（点赞/评分）';

-- 活动表
CREATE TABLE `tb_activity` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
  `user_id` BIGINT NOT NULL COMMENT '创建者ID',
  `title` VARCHAR(200) NOT NULL COMMENT '活动标题',
  `description` TEXT COMMENT '活动描述',
  `cover_image` VARCHAR(500) COMMENT '封面图片',
  `activity_type` TINYINT NOT NULL COMMENT '1-线上活动 2-线下聚会',
  `activity_time` DATETIME NOT NULL COMMENT '活动时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `address` VARCHAR(500) COMMENT '线下地址',
  `online_link` VARCHAR(500) COMMENT '线上链接',
  `max_participants` INT DEFAULT 0 COMMENT '最大参与人数，0-不限',
  `current_participants` INT DEFAULT 0 COMMENT '当前参与人数',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-招募中 2-进行中 3-已结束',
  `labels` JSON COMMENT '活动标签数组',
  `check_in_enabled` TINYINT DEFAULT 1 COMMENT '是否开启打卡',
  `check_in_count` INT DEFAULT 0 COMMENT '打卡人数',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_time_status` (`activity_time`, `status`),
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- =============================================================================
-- AI 数据库
-- =============================================================================
USE `pet_care_ai`;

-- RAG 知识库文档表
CREATE TABLE `tb_knowledge_document` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
  `name` VARCHAR(200) NOT NULL COMMENT '文档名称',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件存储URL',
  `file_type` VARCHAR(20) NOT NULL COMMENT '文件类型：pdf, doc, docx, md, txt等',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
  `version` INT DEFAULT 1 COMMENT '文档版本号',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-有效，0-禁用',
  `chunk_count` INT DEFAULT 0 COMMENT '文档分块数量',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  INDEX `idx_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识库文档表';

-- =============================================================================
-- 执行完成提示
-- =============================================================================
SELECT '数据库建表完成！' AS message;
SELECT 'Core 数据库表数量:' AS info, COUNT(*) AS count FROM information_schema.tables WHERE table_schema = 'pet_care_core';
SELECT 'AI 数据库表数量:' AS info, COUNT(*) AS count FROM information_schema.tables WHERE table_schema = 'pet_care_ai';
