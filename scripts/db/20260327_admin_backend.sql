-- 角色表
CREATE TABLE `tb_role`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_code`   VARCHAR(50)  NOT NULL COMMENT '角色编码（如：admin, user）',
    `role_name`   VARCHAR(100) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '角色描述',
    `sort`        INT          DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除',
    `deleted_at`  DATETIME     DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- 用户角色关联表
CREATE TABLE `tb_user_role`
(
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`    BIGINT NOT NULL COMMENT '用户ID',
    `role_id`    BIGINT NOT NULL COMMENT '角色ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';
INSERT INTO `tb_role` (`role_code`, `role_name`, `description`, `sort`, `status`) VALUES
                                                                                      ('admin', '超级管理员', '系统管理员，拥有所有权限', 1, 1),
                                                                                      ('user', '普通用户', '普通注册用户，拥有基础功能权限', 2, 1);

-- 给已有用户分配默认角色
-- 假设已有用户ID 1 为管理员
INSERT INTO `tb_user_role` (`user_id`, `role_id`) VALUES
                                                      (1, 1),  -- 用户1 拥有 admin 角色
                                                      (1, 2);  -- 用户1 也拥有 user 角色

ALTER TABLE tb_post
    ADD COLUMN audit_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING/APPROVED/REJECTED' AFTER enabled,
    ADD INDEX idx_audit_status (audit_status);

ALTER TABLE tb_activity
    ADD COLUMN audit_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING/APPROVED/REJECTED' AFTER status,
    ADD INDEX idx_audit_status (audit_status);

UPDATE tb_post
SET audit_status = 'APPROVED'
WHERE audit_status IS NULL
   OR audit_status = '';

UPDATE tb_activity
SET audit_status = 'APPROVED'
WHERE audit_status IS NULL
   OR audit_status = '';
