-- 创建核心数据库
CREATE DATABASE IF NOT EXISTS pet_care_core DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 用户表 (users)
CREATE TABLE `tb_users` (
     `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
     `username` varchar(50) NOT NULL COMMENT '用户名',
     `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
     `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
#      `password_hash` varchar(255) NOT NULL COMMENT '加密密码',
     `nickname` varchar(100) DEFAULT NULL COMMENT '昵称',
     `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
     `gender` tinyint(1) DEFAULT '0' COMMENT '性别: 0-未知 1-男 2-女',
     `birthday` date DEFAULT NULL COMMENT '生日',
     `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-禁用 1-正常 2-未激活',
#      `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
#      `last_login_ip` varchar(45) DEFAULT NULL COMMENT '最后登录IP',
     `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     PRIMARY KEY (`id`),
     UNIQUE KEY `uk_username` (`username`),
     UNIQUE KEY `uk_email` (`email`),
     UNIQUE KEY `uk_phone` (`phone`),
     KEY `idx_status` (`status`),
     KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE `tb_pets` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '宠物ID',
    `user_id` bigint(20) NOT NULL COMMENT '用户ID',
    `name` varchar(100) NOT NULL COMMENT '宠物名称',
    `type` tinyint(1) NOT NULL COMMENT '宠物类型: 1-狗 2-猫 3-其他',
    `breed` varchar(100) DEFAULT NULL COMMENT '品种',
    `gender` tinyint(1) DEFAULT '0' COMMENT '性别: 0-未知 1-雄性 2-雌性',
    `birthday` date DEFAULT NULL COMMENT '生日',
    `weight` decimal(5,2) DEFAULT NULL COMMENT '体重(kg)',
    `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像URL',
    `is_sterilized` tinyint(1) DEFAULT '0' COMMENT '是否绝育: 0-否 1-是',
    `health_notes` text COMMENT '健康备注',
    `allergy_info` text COMMENT '过敏信息',
    `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态: 0-删除 1-正常',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_type` (`type`),
    KEY `idx_created_at` (`created_at`),
    CONSTRAINT `fk_tb_pet_user` FOREIGN KEY (`user_id`) REFERENCES `tb_users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物表';

CREATE TABLE `tb_pet_vaccinations` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `pet_id` bigint(20) NOT NULL COMMENT '宠物ID',
    `vaccine_name` varchar(200) NOT NULL COMMENT '疫苗名称',
    `vaccination_date` date NOT NULL COMMENT '接种日期',
    `next_due_date` date DEFAULT NULL COMMENT '下次接种日期',
    `notes` text COMMENT '备注',
    `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件URL',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_pet_id` (`pet_id`),
    KEY `idx_vaccination_date` (`vaccination_date`),
    CONSTRAINT `fk_tb_vaccination_pet` FOREIGN KEY (`pet_id`) REFERENCES `tb_pets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物疫苗记录表';

CREATE TABLE `tb_pet_health_records` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `pet_id` bigint(20) NOT NULL COMMENT '宠物ID',
    `record_type` tinyint(1) NOT NULL COMMENT '记录类型: 1-体重 2-体温 3-症状 4-用药 5-其他',
    `value` decimal(8,2) DEFAULT NULL COMMENT '记录数值',
    `unit` varchar(20) DEFAULT NULL COMMENT '单位',
    `notes` text COMMENT '备注',
    `record_date` datetime NOT NULL COMMENT '记录时间',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_pet_id` (`pet_id`),
    KEY `idx_record_type` (`record_type`),
    KEY `idx_record_date` (`record_date`),
    CONSTRAINT `fk_tb_health_pet` FOREIGN KEY (`pet_id`) REFERENCES `tb_pets` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物健康记录表';