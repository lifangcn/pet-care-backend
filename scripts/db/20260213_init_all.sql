-- {@code description}: 宠物关怀系统 - 完整建表脚本
-- {@code date}: 2026-02-13
-- {@code author}: Michael
--
-- 说明：
-- 1. 包含所有模块的表（core、ai）
-- 2. 所有表统一使用 is_deleted 和 deleted_at 字段进行逻辑删除

-- =============================================================================
-- 创建数据库
-- =============================================================================

DROP DATABASE IF EXISTS `pet_care_core`;
DROP DATABASE IF EXISTS `pet_care_ai`;
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
CREATE TABLE `tb_user`
(
    `id`         BIGINT AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50) NOT NULL COMMENT '用户名',
    `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`     VARCHAR(500) DEFAULT NULL COMMENT '用户头像URL',
    `enabled`    TINYINT      DEFAULT 1 COMMENT '是否启用：1-正常，0-禁用',
    `address`    VARCHAR(200) DEFAULT NULL COMMENT '用户地址',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at` DATETIME     DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- -----------------------------------------------------------------------------
-- 2. 宠物相关表
-- -----------------------------------------------------------------------------

-- 宠物表
CREATE TABLE `tb_pet`
(
    `id`           BIGINT AUTO_INCREMENT COMMENT '宠物ID',
    `user_id`      BIGINT      NOT NULL COMMENT '所属用户ID',
    `name`         VARCHAR(50) NOT NULL COMMENT '宠物名字',
    `type`         VARCHAR(20)   DEFAULT NULL COMMENT '宠物类型',
    `breed`        VARCHAR(50)   DEFAULT NULL COMMENT '宠物品种',
    `gender`       VARCHAR(16)   DEFAULT NULL COMMENT '性别：MALE-公 FEMALE-母',
    `birthday`     DATE          DEFAULT NULL COMMENT '生日',
    `weight`       DECIMAL(5, 2) DEFAULT NULL COMMENT '体重(kg)',
    `avatar`       VARCHAR(500)  DEFAULT NULL COMMENT '宠物头像URL',
    `health_notes` VARCHAR(500)  DEFAULT NULL COMMENT '健康备注',
    `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`   DATETIME      DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='宠物表';

-- 健康记录表
CREATE TABLE `tb_health_record`
(
    `id`              BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `pet_id`          BIGINT      NOT NULL COMMENT '宠物ID',
    `user_id`         BIGINT      NOT NULL COMMENT '用户ID',
    `record_type`     VARCHAR(20) NOT NULL COMMENT '记录类型: WEIGHT(体重), TEMPERATURE(体温), MEDICAL(用药)',
    `title`           VARCHAR(200)   DEFAULT NULL COMMENT '标题',
    `description`     TEXT COMMENT '描述',
    `record_time`     DATETIME    NOT NULL COMMENT '记录时间',
    `value`           DECIMAL(10, 2) DEFAULT NULL COMMENT '数值(体重/体温等)',
    `symptom`         VARCHAR(500)   DEFAULT NULL COMMENT '症状信息',
    `medication_info` VARCHAR(500)   DEFAULT NULL COMMENT '用药信息',
    `created_at`      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT(1)     DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`      DATETIME       DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_pet_id` (`pet_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='健康记录表';

-- -----------------------------------------------------------------------------
-- 3. 提醒相关表
-- -----------------------------------------------------------------------------

-- 提醒事件表
CREATE TABLE `tb_reminder`
(
    `id`                    BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `pet_id`                BIGINT      NOT NULL COMMENT '宠物ID',
    `user_id`               BIGINT      NOT NULL COMMENT '用户ID',
    `source_type`           VARCHAR(20) NOT NULL COMMENT '记录来源：manual, health_record, system',
    `source_id`             BIGINT       DEFAULT NULL COMMENT '来源ID（如健康记录ID）',
    `title`                 VARCHAR(200) DEFAULT NULL COMMENT '标题',
    `description`           TEXT COMMENT '描述',
    `record_time`           DATETIME    NOT NULL COMMENT '记录时间',
    `schedule_time`         DATETIME     DEFAULT NULL COMMENT '计划时间(用于提醒)',
    `remind_before_minutes` INT          DEFAULT 0 COMMENT '提前提醒时间(分钟)',
    `repeat_type`           VARCHAR(20)  DEFAULT 'none' COMMENT '重复类型: NONE(不重复), DAILY(每天), WEEKLY(每周), MONTHLY(每月), CUSTOM(自定义)',
    `repeat_config`         JSON         DEFAULT NULL COMMENT '重复配置(自定义重复规则)',
    `is_active`             TINYINT(1)   DEFAULT 1 COMMENT '是否激活',
    `total_occurrences`     INT          DEFAULT 0 COMMENT '总执行次数',
    `completed_count`       INT          DEFAULT 0 COMMENT '已完成次数',
    `completed_time`        DATETIME     DEFAULT NULL COMMENT '完成时间',
    `created_at`            DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`            DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `next_trigger_time`     DATETIME     DEFAULT NULL COMMENT '下一次触发时间（内部调度使用，用户不可见）',
    `reminder_execution_id` BIGINT       DEFAULT NULL COMMENT '提醒执行记录ID，当前提醒和执行记录关联，标识最新的执行情况',
    `is_deleted`            TINYINT(1)   DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`            DATETIME     DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_pet_id` (`pet_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_schedule_time` (`schedule_time`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='提醒事件表';

-- 提醒执行记录表
CREATE TABLE `tb_reminder_execution`
(
    `id`                BIGINT AUTO_INCREMENT COMMENT '主键ID',
    `reminder_id`       BIGINT   NOT NULL COMMENT '提醒ID',
    `pet_id`            BIGINT   NOT NULL COMMENT '宠物ID',
    `user_id`           BIGINT   NOT NULL COMMENT '用户ID',
    `schedule_time`     DATETIME NOT NULL COMMENT '计划执行时间',
    `actual_time`       DATETIME    DEFAULT NULL COMMENT '实际执行时间',
    `status`            VARCHAR(16) DEFAULT 'PENDING' COMMENT '执行状态：PENDING-待处理 COMPLETED-已完成 OVERDUE-已过期',
    `completion_notes`  TEXT COMMENT '完成说明',
    `notification_time` DATETIME NOT NULL COMMENT '通知时间',
    `is_read`           TINYINT(1)  DEFAULT 0 COMMENT '是否已读',
    `is_sent`           TINYINT(1)  DEFAULT 0 COMMENT '是否已发送',
    `sent_at`           DATETIME    DEFAULT NULL COMMENT '发送时间',
    `read_at`           DATETIME    DEFAULT NULL COMMENT '阅读时间',
    `created_at`        DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`        TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`        DATETIME    DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_reminder_id` (`reminder_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_schedule_time` (`schedule_time`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 0
  DEFAULT CHARSET = utf8mb4 COMMENT ='提醒执行记录表';

-- -----------------------------------------------------------------------------
-- 4. 社交相关表
-- -----------------------------------------------------------------------------

-- 动态表
CREATE TABLE `tb_post`
(
    `id`               BIGINT AUTO_INCREMENT COMMENT '动态ID',
    `user_id`          BIGINT      NOT NULL COMMENT '发布者ID',
    `title`            VARCHAR(200) COMMENT '标题',
    `content`          TEXT COMMENT '内容描述',
    `post_type`        VARCHAR(16) NOT NULL COMMENT '类型：PRODUCT_SHARE-好物分享 SERVICE_RECO-服务推荐 LOCATION_RECO-地点推荐 DAILY-日常分享 ACTIVITY_CHECK-活动打卡',
    `media_urls`       JSON COMMENT '图片/视频URL数组',
    `external_link`    VARCHAR(500) COMMENT '外部链接（商品、服务、地图）',
    `location_address` VARCHAR(500) COMMENT '地点信息',
    `price_range`      VARCHAR(50) COMMENT '价格区间（如：100-200元）',
    `like_count`       INT           DEFAULT 0 COMMENT '点赞数',
    `rating_count`     INT           DEFAULT 0 COMMENT '评分次数',
    `rating_total`     INT           DEFAULT 0 COMMENT '评分总分',
    `rating_avg`       DECIMAL(3, 2) DEFAULT 0 COMMENT '平均分',
    `view_count`       INT           DEFAULT 0 COMMENT '浏览数',
    `enabled`          TINYINT       DEFAULT 1 COMMENT '是否启用：1-正常，0-隐藏',
    `activity_id`      BIGINT        DEFAULT NULL COMMENT '关联的活动ID',
    `created_at`       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT(1)    DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`       DATETIME      DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_activity_id` (`activity_id`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='动态表';

-- 标签表
CREATE TABLE `tb_label`
(
    `id`             BIGINT AUTO_INCREMENT COMMENT '标签ID',
    `name`           VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名',
    `type`           VARCHAR(16) DEFAULT 'GENERAL' COMMENT '类型：GENERAL-通用标签 BREED-宠物品种 CONTENT-内容类型',
    `icon`           VARCHAR(200) COMMENT '标签图标',
    `color`          VARCHAR(20) COMMENT '标签颜色',
    `use_count`      INT         DEFAULT 0 COMMENT '使用次数',
    `is_recommended` TINYINT     DEFAULT 0 COMMENT '是否推荐标签',
    `status`         TINYINT     DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    `created_at`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`     DATETIME    DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='标签表';

-- 动态标签关联表
CREATE TABLE `tb_post_label`
(
    `id`         BIGINT AUTO_INCREMENT COMMENT '关联ID',
    `post_id`    BIGINT NOT NULL COMMENT '动态ID',
    `label_id`   BIGINT NOT NULL COMMENT '标签ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_label` (`post_id`, `label_id`),
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_label_id` (`label_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='动态标签关联表';

-- 互动表（点赞+评分）
CREATE TABLE `tb_interaction`
(
    `id`               BIGINT AUTO_INCREMENT COMMENT '互动ID',
    `user_id`          BIGINT      NOT NULL COMMENT '用户ID',
    `post_id`          BIGINT      NOT NULL COMMENT '动态ID',
    `interaction_type` VARCHAR(16) NOT NULL COMMENT '类型：LIKE-点赞 RATING-评分',
    `rating_value`     TINYINT COMMENT '评分值 1-5',
    `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post_type` (`user_id`, `post_id`, `interaction_type`),
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='互动表（点赞/评分）';

-- 活动表
CREATE TABLE `tb_activity`
(
    `id`                   BIGINT AUTO_INCREMENT COMMENT '活动ID',
    `user_id`              BIGINT       NOT NULL COMMENT '创建者ID',
    `title`                VARCHAR(200) NOT NULL COMMENT '活动标题',
    `description`          TEXT COMMENT '活动描述',
    `cover_image`          VARCHAR(500) COMMENT '封面图片',
    `activity_type`        VARCHAR(16)  NOT NULL COMMENT '类型：ONLINE-线上活动 OFFLINE-线下聚会',
    `activity_time`        DATETIME     NOT NULL COMMENT '活动时间',
    `end_time`             DATETIME    DEFAULT NULL COMMENT '结束时间',
    `address`              VARCHAR(500) COMMENT '线下地址',
    `online_link`          VARCHAR(500) COMMENT '线上链接',
    `max_participants`     INT         DEFAULT 0 COMMENT '最大参与人数，0-不限',
    `current_participants` INT         DEFAULT 0 COMMENT '当前参与人数',
    `status`               VARCHAR(16) DEFAULT 'RECRUITING' COMMENT '状态：RECRUITING-招募中 ONGOING-进行中 ENDED-已结束',
    `labels`               JSON COMMENT '活动标签数组',
    `check_in_enabled`     TINYINT     DEFAULT 1 COMMENT '是否开启打卡',
    `check_in_count`       INT         DEFAULT 0 COMMENT '打卡人数',
    `created_at`           DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`           TINYINT(1)  DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`           DATETIME    DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_time_status` (`activity_time`, `status`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='活动表';

-- 用户积分账户表
CREATE TABLE `tb_points_account`
(
    `id`               BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    `user_id`          BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `available_points` INT             NOT NULL DEFAULT 0 COMMENT '当前可用积分',
    `total_points`     INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '历史累计获取积分（用于等级计算，只增不减）',
    `created_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`       TINYINT(1)               DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`       DATETIME                 DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户积分账户表';

-- 积分流水记录表
CREATE TABLE `tb_points_record`
(
    `id`            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `points`        INT             NOT NULL COMMENT '积分变动值（正为获取，负为消耗）',
    `points_before` INT             NOT NULL COMMENT '变动前积分',
    `points_after`  INT             NOT NULL COMMENT '变动后积分',
    `action_type`   VARCHAR(16)     NOT NULL COMMENT '行为类型：REGISTER-注册赠送 CHECK_IN-签到 PUBLISH-发布内容 COMMENT-评论 LIKE-点赞他人 LIKED-被点赞 COMMENTED-被评论 AI_CONSULT-AI健康咨询 COUPON_REDEEM-代金券兑换',
    `biz_type`      VARCHAR(32)              DEFAULT NULL COMMENT '关联业务类型',
    `biz_id`        BIGINT UNSIGNED          DEFAULT NULL COMMENT '关联业务ID',
    `coupon_id`     BIGINT UNSIGNED          DEFAULT NULL COMMENT '使用的代金券ID',
    `coupon_deduct` INT UNSIGNED             DEFAULT NULL COMMENT '代金券抵扣积分数',
    `remark`        VARCHAR(255)             DEFAULT NULL COMMENT '备注说明',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='积分流水记录表';

-- 积分代金券模板表
CREATE TABLE `tb_points_coupon_template`
(
    `id`             BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    `name`           VARCHAR(64)         NOT NULL COMMENT '券名称',
    `face_value`     INT UNSIGNED        NOT NULL COMMENT '面值（可抵扣积分数）',
    `valid_days`     INT UNSIGNED        NOT NULL COMMENT '有效天数（领取后计算）',
    `total_count`    INT UNSIGNED        NOT NULL DEFAULT 0 COMMENT '发放总量（0表示不限）',
    `issued_count`   INT UNSIGNED        NOT NULL DEFAULT 0 COMMENT '已发放数量',
    `per_user_limit` INT UNSIGNED        NOT NULL DEFAULT 1 COMMENT '每人限领数量',
    `source_type`    VARCHAR(16)         NOT NULL DEFAULT 'SYSTEM' COMMENT '来源类型：SYSTEM-系统发放 ACTIVITY-活动发放 NEWBIE-新人礼包',
    `status`         TINYINT(1) UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    `created_at`     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT(1)                   DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`     DATETIME                     DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='积分代金券模板表';

-- 用户积分代金券表
CREATE TABLE `tb_points_coupon`
(
    `id`             BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    `template_id`    BIGINT UNSIGNED NOT NULL COMMENT '券模板ID',
    `face_value`     INT UNSIGNED    NOT NULL COMMENT '面值（冗余，防止模板变更）',
    `status`         VARCHAR(16)     NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED-未使用 USED-已使用 EXPIRED-已过期',
    `start_time`     DATETIME        NOT NULL COMMENT '生效时间',
    `end_time`       DATETIME        NOT NULL COMMENT '失效时间',
    `used_time`      DATETIME                 DEFAULT NULL COMMENT '使用时间',
    `used_record_id` BIGINT UNSIGNED          DEFAULT NULL COMMENT '使用时关联的流水ID',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`     TINYINT(1)               DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`     DATETIME                 DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户积分代金券表';

-- =============================================================================
-- AI 数据库
-- =============================================================================
USE `pet_care_ai`;

-- RAG 知识库文档表
CREATE TABLE `tb_knowledge_document`
(
    `id`          BIGINT AUTO_INCREMENT COMMENT '文档ID',
    `name`        VARCHAR(200) NOT NULL COMMENT '文档名称',
    `file_url`    VARCHAR(500) NOT NULL COMMENT '文件存储URL',
    `file_type`   VARCHAR(20)  NOT NULL COMMENT '文件类型：pdf, doc, docx, md, txt等',
    `file_size`   BIGINT       NOT NULL COMMENT '文件大小（字节）',
    `version`     INT        DEFAULT 1 COMMENT '文档版本号',
    `status`      TINYINT    DEFAULT 1 COMMENT '状态：1-有效，0-禁用',
    `chunk_count` INT        DEFAULT 0 COMMENT '文档分块数量',
    `created_at`  DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`  DATETIME   DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='RAG知识库文档表';

-- =============================================================================
-- 执行完成提示
-- =============================================================================
SELECT '数据库建表完成！' AS message;
SELECT 'Core 数据库表数量:' AS info, COUNT(*) AS count
FROM information_schema.tables
WHERE table_schema = 'pet_care_core';
SELECT 'AI 数据库表数量:' AS info, COUNT(*) AS count
FROM information_schema.tables
WHERE table_schema = 'pet_care_ai';

-- =============================================================================
-- 初始化数据: 插入新注册用户积分劵
-- =============================================================================
INSERT INTO `pet_care_core`.`tb_points_coupon_template` (`name`, `face_value`, `valid_days`, `total_count`,
                                                         `issued_count`, `per_user_limit`, `source_type`, `status`)
VALUES ('新人注册券', 1000, 365, 100000, 0, 1, 'NEWCOMER', 1)
ON DUPLICATE KEY UPDATE `name`           = VALUES(`name`),
                        `face_value`     = VALUES(`face_value`),
                        `valid_days`     = VALUES(`valid_days`),
                        `per_user_limit` = VALUES(`per_user_limit`);

