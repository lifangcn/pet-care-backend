-- 核心服务数据库
CREATE DATABASE IF NOT EXISTS `pet_care_core` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `pet_care_core`;
-- 1.用户表
CREATE TABLE `tb_user`
(
    `id`         BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    `phone`      VARCHAR(20) UNIQUE COMMENT '手机号',
    `nickname`   VARCHAR(50) COMMENT '昵称',
    `avatar`     VARCHAR(500) COMMENT '用户头像URL',
    `status`     TINYINT  DEFAULT 1 COMMENT '用户状态：1-正常，0-禁用',
    `address`    VARCHAR(200) COMMENT '用户地址',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 2.宠物表
CREATE TABLE `tb_pet`
(
    `id`           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '宠物ID',
    `user_id`      BIGINT      NOT NULL COMMENT '所属用户ID',
    `name`         VARCHAR(50) NOT NULL COMMENT '宠物名字',
    `type`         VARCHAR(20) COMMENT '宠物类型',
    `breed`        VARCHAR(50) COMMENT '宠物品种',
    `gender`       TINYINT COMMENT '性别：1-公，0-母',
    `birthday`     DATE COMMENT '生日',
    `weight`       DECIMAL(5, 2) COMMENT '体重(kg)',
    `avatar`       VARCHAR(500) COMMENT '宠物头像URL',
    `health_notes` VARCHAR(500) COMMENT '健康备注',
    `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='宠物表';

-- 3.健康状况记录表（异常/指标记录）
CREATE TABLE `tb_health_record`
(
    `id`              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `pet_id`          BIGINT      NOT NULL COMMENT '宠物ID',
    `user_id`         BIGINT      NOT NULL COMMENT '用户ID',
    `record_type`     VARCHAR(20) NOT NULL COMMENT '记录类型: weight(体重), temperature(体温), medical(用药)',
    `title`           VARCHAR(200) COMMENT '标题',
    `description`     TEXT COMMENT '描述',
    `record_time`     DATETIME    NOT NULL COMMENT '记录时间',
    -- 记录指标字段
    `value`           DECIMAL(10, 2) COMMENT '数值(体重/体温等)',
    -- 用药信息字段
    `symptom`         VARCHAR(500) COMMENT '症状信息',
    `medication_info` VARCHAR(500) COMMENT '用药信息',
    `created_at`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='健康记录表';

-- 4.提醒事件表
CREATE TABLE `tb_reminder`
(
    `id`                    BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `pet_id`                BIGINT      NOT NULL COMMENT '宠物ID',
    `user_id`               BIGINT      NOT NULL COMMENT '用户ID',
    `source_type`           VARCHAR(20) NOT NULL COMMENT '记录来源：manual, health_record, system',
    `source_id`             BIGINT      NOT NULL COMMENT '来源ID（如健康记录ID）',
    `title`                 VARCHAR(200) COMMENT '标题',
    `description`           TEXT COMMENT '描述',
    `record_time`           DATETIME    NOT NULL COMMENT '记录时间',
    `next_trigger_time` DATETIME COMMENT '下一次触发时间（内部调度使用，用户不可见）',
    `schedule_time`         DATETIME COMMENT '计划时间(用于提醒)',
    `remind_before_minutes` INT         DEFAULT 0 COMMENT '提前提醒时间(分钟)',
    `repeat_type`           VARCHAR(20) DEFAULT 'none' COMMENT '重复类型: none(不重复), daily(每天), weekly(每周), monthly(每月), custom(自定义)',
    `repeat_config`         JSON COMMENT '重复配置(自定义重复规则)',
    `is_active`             BOOLEAN     DEFAULT TRUE COMMENT '是否激活',
    `total_occurrences`     INT         DEFAULT 0 COMMENT '总执行次数',
    `completed_count`       INT         DEFAULT 0 COMMENT '已完成次数',
    `completed_time`        DATETIME COMMENT '完成时间',
    `created_at`            DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='提醒事件表';

-- 5.提醒执行记录表
CREATE TABLE `tb_reminder_execution`
(
    `id`                BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    `reminder_id`       BIGINT   NOT NULL COMMENT '提醒ID',
    `pet_id`            BIGINT   NOT NULL COMMENT '宠物ID',
    `user_id`           BIGINT   NOT NULL COMMENT '用户ID',
    `schedule_time` DATETIME NOT NULL COMMENT '计划执行时间',
    `actual_time`       DATETIME COMMENT '实际执行时间',
    `status`            ENUM ('pending', 'completed', 'overdue') DEFAULT 'pending' COMMENT '执行状态',
    `completion_notes`  TEXT COMMENT '完成说明',
    `notification_time` DATETIME NOT NULL COMMENT '通知时间',
    `is_read`           TINYINT(1)                               DEFAULT 0 COMMENT '是否已读',
    `is_sent`           TINYINT(1)                               DEFAULT 0 COMMENT '是否已发送',
    `sent_at`           DATETIME COMMENT '发送时间',
    `read_at`           DATETIME COMMENT '阅读时间',
    `created_at`        DATETIME                                 DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='提醒执行记录表';
