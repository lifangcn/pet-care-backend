-- =============================================
-- 积分体系数据库表设计
-- =============================================

-- 用户积分账户表
CREATE TABLE `tb_points_account` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `available_points` INT NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    `total_points` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '历史累计获取积分（用于等级计算，只增不减）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分账户表';

-- 积分流水记录表
CREATE TABLE `tb_points_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `points` INT NOT NULL COMMENT '积分变动值（正为获取，负为消耗）',
    `points_before` INT NOT NULL COMMENT '变动前积分',
    `points_after` INT NOT NULL COMMENT '变动后积分',
    `action_type` VARCHAR(16) NOT NULL COMMENT '行为类型：REGISTER-注册赠送 CHECK_IN-签到 PUBLISH-发布内容 COMMENT-评论 LIKE-点赞他人 LIKED-被点赞 COMMENTED-被评论 AI_CONSULT-AI健康咨询 COUPON_REDEEM-代金券兑换',
    `biz_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型',
    `biz_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务ID',
    `coupon_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '使用的代金券ID',
    `coupon_deduct` INT UNSIGNED DEFAULT NULL COMMENT '代金券抵扣积分数',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注说明',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水记录表';

-- 积分代金券模板表
CREATE TABLE `tb_points_coupon_template` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(64) NOT NULL COMMENT '券名称',
    `face_value` INT UNSIGNED NOT NULL COMMENT '面值（可抵扣积分数）',
    `valid_days` INT UNSIGNED NOT NULL COMMENT '有效天数（领取后计算）',
    `total_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '发放总量（0表示不限）',
    `issued_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已发放数量',
    `per_user_limit` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    `source_type` VARCHAR(16) NOT NULL DEFAULT 'SYSTEM' COMMENT '来源类型：SYSTEM-系统发放 ACTIVITY-活动发放 NEWCOMER-新人礼包',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分代金券模板表';

-- 用户积分代金券表
CREATE TABLE `tb_points_coupon` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `template_id` BIGINT UNSIGNED NOT NULL COMMENT '券模板ID',
    `face_value` INT UNSIGNED NOT NULL COMMENT '面值（冗余，防止模板变更）',
    `status` VARCHAR(16) NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用 USED-已使用 EXPIRED-已过期',
    `start_time` DATETIME NOT NULL COMMENT '生效时间',
    `end_time` DATETIME NOT NULL COMMENT '失效时间',
    `used_time` DATETIME DEFAULT NULL COMMENT '使用时间',
    `used_record_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '使用时关联的流水ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分代金券表';
