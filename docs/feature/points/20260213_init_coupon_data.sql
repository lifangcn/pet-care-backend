-- ============================================
-- 积分代金券初始化数据
-- Created: 2026/02/13
-- Author: Michael
-- ============================================

-- 插入新人注册券模板（source_type='NEWCOMER'）
-- 面值1000积分，有效期365天，每人限领1张
INSERT INTO `tb_points_coupon_template` (
    `name`,
    `face_value`,
    `valid_days`,
    `total_count`,
    `issued_count`,
    `per_user_limit`,
    `source_type`,
    `status`
) VALUES (
    '新人注册券',
    1000,
    365,
    0,
    0,
    1,
    'NEWCOMER',
    1
) ON DUPLICATE KEY UPDATE
    `name` = VALUES(`name`),
    `face_value` = VALUES(`face_value`),
    `valid_days` = VALUES(`valid_days`),
    `per_user_limit` = VALUES(`per_user_limit`);
