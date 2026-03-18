#!/bin/bash

# 生成测试用户数据脚本
# 创建时间: 2025-02-09
# 作者: Michael Li
# 修改: 2026-02-11 - 分离单用户和多用户测试数据
#
# 生成两个独立的 CSV 文件：
#   - users_multi.csv: 多用户场景（userId 1~1000）
#   - users_single.csv: 单用户场景（userId 1001，避免与多用户冲突）
#

OUTPUT_MULTI="users_multi.csv"
OUTPUT_SINGLE="users_single.csv"
USER_COUNT=1000
SINGLE_USER_ID=1001

echo "========================================"
echo "生成测试用户数据"
echo "========================================"

# 生成多用户数据（userId 1~1000）
echo "userId" > $OUTPUT_MULTI
for i in $(seq 1 $USER_COUNT)
do
    echo "$i" >> $OUTPUT_MULTI
done

echo ""
echo "生成单用户数据（userId $SINGLE_USER_ID，独立于多用户场景）"
echo "$SINGLE_USER_ID" > $OUTPUT_SINGLE

echo ""
echo "========================================"
echo "已生成 $OUTPUT_MULTI（$USER_COUNT 个用户）"
echo "已生成 $OUTPUT_SINGLE（单用户测试，userId=$SINGLE_USER_ID）"
echo "========================================"
echo ""
echo "下一步：初始化数据库"
echo ""
echo "方式一：使用 MySQL 客户端执行 SQL"
echo "  mysql -u root -p petcare < init_test_data.sql"
echo ""
echo "方式二：使用 Docker"
echo "  docker exec -i <mysql-container> mysql -u root -p petcare < init_test_data.sql"
echo ""
echo "方式三：直接复制 SQL 执行"
echo "  查看文件内容：cat init_test_data.sql"
echo ""
echo "确认应用已启用测试模式:"
echo "  java -Djwt.test.mode=true -jar pet-care-core.jar"
echo ""
