-- 创建Canal用户
CREATE USER IF NOT EXISTS 'cdc'@'%' IDENTIFIED BY '123456';

-- 授予复制权限（读取binlog必需）
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'cdc'@'%';

-- 授予数据库访问权限
GRANT ALL PRIVILEGES ON pet_care_core.* TO 'cdc'@'%';
GRANT ALL PRIVILEGES ON pet_care_ai.* TO 'cdc'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
