-- 初始化测试数据
-- 创建时间: 2025-02-11
-- 作者: Michael Li
-- 修改: 使用反引号避免 count 保留字冲突，增加初始积分

-- 1. 创建 1001 个测试用户（tb_user）
-- 注意：id 是自增的，先插入数据后再查询 id
INSERT INTO tb_user (phone, username, nickname, avatar, enabled)
SELECT
    CONCAT('1380000', LPAD(seq, 4, '0')) AS phone,
    CONCAT('test_user_', seq) AS username,
    CONCAT('测试用户', seq) AS nickname,
    'https://example.com/avatar/default.png' AS avatar, true
FROM (
    SELECT @row := @row + 1 AS seq
    FROM (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t1,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t2,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t3,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t4,
         (SELECT 0 UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) t5,
         (SELECT @row:=0) r
    LIMIT 1001
) seq;

-- 2. 为 1001 个用户创建积分账户
-- 初始积分 100000，支持约 10000 次消耗（每次 -10）
INSERT INTO tb_points_account (user_id, available_points, total_points, created_at, updated_at)
SELECT
    id AS user_id,
    100000 AS available_points,
    100000 AS total_points,
    NOW() AS created_at,
    NOW() AS updated_at
FROM tb_user
WHERE id <= 1001
ORDER BY id;

-- 3. 验证数据（使用反引号避免 count 保留字冲突）
SELECT '用户数量' AS label, COUNT(*) AS user_count FROM tb_user WHERE id <= 1000
UNION ALL
SELECT '积分账户数量' AS label, COUNT(*) AS account_count FROM tb_points_account WHERE user_id <= 1000;
