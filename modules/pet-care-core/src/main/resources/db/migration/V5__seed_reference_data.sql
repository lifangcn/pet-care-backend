INSERT INTO petcare.tb_role (role_code, role_name, description, sort, status)
SELECT role_code, role_name, description, sort, status
FROM (
    VALUES
        ('admin', '超级管理员', '系统管理员，拥有所有权限', 1, 1),
        ('user', '普通用户', '普通注册用户，拥有基础功能权限', 2, 1)
) AS seed(role_code, role_name, description, sort, status)
WHERE NOT EXISTS (
    SELECT 1
    FROM petcare.tb_role role
    WHERE lower(role.role_code) = lower(seed.role_code)
);

INSERT INTO petcare.tb_points_coupon_template (
    name,
    face_value,
    valid_days,
    total_count,
    issued_count,
    per_user_limit,
    source_type,
    status
)
SELECT
    '新人注册券',
    1000,
    365,
    100000,
    0,
    1,
    'NEWCOMER',
    1
WHERE NOT EXISTS (
    SELECT 1
    FROM petcare.tb_points_coupon_template
    WHERE name = '新人注册券'
      AND source_type = 'NEWCOMER'
);
