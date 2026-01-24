-- 创建Canal用户
CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED BY 'canal';

-- 授予复制权限（读取binlog必需）
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';

-- 授予数据库访问权限
GRANT ALL PRIVILEGES ON pet_care_core.* TO 'canal'@'%';
GRANT ALL PRIVILEGES ON pet_care_ai.* TO 'canal'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
