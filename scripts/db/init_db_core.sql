-- 核心服务数据库
CREATE DATABASE IF NOT EXISTS `pet_care_core` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pet_care_core`;

-- 用户表
CREATE TABLE `tb_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '用户头像URL',
  `status` tinyint DEFAULT '1' COMMENT '用户状态：1-正常，0-禁用',
  `address` varchar(200) DEFAULT NULL COMMENT '用户地址',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `deleted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 宠物表
CREATE TABLE `tb_pet` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '宠物ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `name` varchar(50) NOT NULL COMMENT '宠物名字',
  `type` varchar(20) DEFAULT NULL COMMENT '宠物类型',
  `breed` varchar(50) DEFAULT NULL COMMENT '宠物品种',
  `gender` tinyint DEFAULT NULL COMMENT '性别：1-公，0-母',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
  `avatar` varchar(500) DEFAULT NULL COMMENT '宠物头像URL',
  `health_notes` varchar(500) DEFAULT NULL COMMENT '健康备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `deleted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='宠物表';

-- 健康记录表
CREATE TABLE `tb_health_record` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` bigint NOT NULL COMMENT '宠物ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_type` varchar(20) NOT NULL COMMENT '记录类型: weight(体重), temperature(体温), medical(用药)',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `description` text COMMENT '描述',
  `record_time` datetime NOT NULL COMMENT '记录时间',
  `value` decimal(10,2) DEFAULT NULL COMMENT '数值(体重/体温等)',
  `symptom` varchar(500) DEFAULT NULL COMMENT '症状信息',
  `medication_info` varchar(500) DEFAULT NULL COMMENT '用药信息',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `deleted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='健康记录表';

-- 提醒事件表
CREATE TABLE `tb_reminder` (
  `id` bigint PRIMARY KEY  AUTO_INCREMENT COMMENT '主键ID',
  `pet_id` bigint NOT NULL COMMENT '宠物ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `source_type` varchar(20) NOT NULL COMMENT '记录来源：manual, health_record, system',
  `source_id` bigint DEFAULT NULL COMMENT '来源ID（如健康记录ID）',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `description` text COMMENT '描述',
  `record_time` datetime NOT NULL COMMENT '记录时间',
  `schedule_time` datetime DEFAULT NULL COMMENT '计划时间(用于提醒)',
  `remind_before_minutes` int DEFAULT '0' COMMENT '提前提醒时间(分钟)',
  `repeat_type` varchar(20) DEFAULT 'none' COMMENT '重复类型: none(不重复), daily(每天), weekly(每周), monthly(每月), custom(自定义)',
  `repeat_config` json DEFAULT NULL COMMENT '重复配置(自定义重复规则)',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否激活',
  `total_occurrences` int DEFAULT '0' COMMENT '总执行次数',
  `completed_count` int DEFAULT '0' COMMENT '已完成次数',
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `next_trigger_time` datetime DEFAULT NULL COMMENT '下一次触发时间（内部调度使用，用户不可见）',
  `reminder_execution_id` bigint DEFAULT NULL COMMENT '提醒执行记录ID，当前提醒和执行记录关联，标识最新的执行情况',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `deleted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='提醒事件表';

-- 提醒执行记录表
CREATE TABLE `tb_reminder_execution` (
  `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  `reminder_id` bigint NOT NULL COMMENT '提醒ID',
  `pet_id` bigint NOT NULL COMMENT '宠物ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `schedule_time` datetime NOT NULL COMMENT '计划执行时间',
  `actual_time` datetime DEFAULT NULL COMMENT '实际执行时间',
  `status` enum('pending','completed','overdue') DEFAULT 'pending' COMMENT '执行状态',
  `completion_notes` text COMMENT '完成说明',
  `notification_time` datetime NOT NULL COMMENT '通知时间',
  `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
  `is_sent` tinyint(1) DEFAULT '0' COMMENT '是否已发送',
  `sent_at` datetime DEFAULT NULL COMMENT '发送时间',
  `read_at` datetime DEFAULT NULL COMMENT '阅读时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `deleted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '删除时间'
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COMMENT='提醒执行记录表';