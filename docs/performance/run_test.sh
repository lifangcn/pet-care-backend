#!/bin/bash

# 一键执行性能测试脚本
# 创建时间: 2026-02-11
# 作者: Michael Li

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}========================================"
echo -e "${GREEN}积分系统性能测试 - 一键执行${NC}"
echo -e "${GREEN}========================================${NC}"

cd /Users/michael/IdeaProjects/petcare/doc/performance

# 检查 JAR 文件
JAR_FILE="/Users/michael/IdeaProjects/petcare/modules/pet-care-core/target/pet-care-core.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo -e "${YELLOW}JAR 文件不存在，正在编译...${NC}"
    cd /Users/michael/IdeaProjects/petcare
    mvn clean package -DskipTests -q
    cd /Users/michael/IdeaProjects/petcare/doc/performance
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}编译成功，JAR 文件已生成${NC}"
    else
        echo -e "${RED}编译失败，请检查错误${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}JAR 文件已就绪${NC}"
fi

# 检查应用状态
echo -e "${GREEN}检查应用状态...${NC}"

# 检测端口
if curl -s http://localhost:8080/actuator/health 2>&1 | grep -q "UP" > /dev/null; then
    APP_STATUS="running"
else
    APP_STATUS="stopped"
fi

# 应用状态判断
if [ "$APP_STATUS" = "running" ]; then
    echo -e "${GREEN}应用运行中，将直接进行压测${NC}"
    echo ""
    echo -e "${YELLOW}提示：如需重启应用，请按 Ctrl+C 停止${NC}"
else
    echo ""
    echo -e "${YELLOW}应用未运行${NC}"
    echo -e "${YELLOW}正在尝试启动应用（后台）...${NC}"

    # 后台启动应用
    cd /Users/michael/IdeaProjects/petcare
    java -Djwt.test.mode=true -jar modules/pet-care-core/target/pet-care-core.jar > /dev/null 2>&1 &
    APP_PID=$!

    # 等待应用启动
    echo -e "${YELLOW}等待应用启动（10秒）...${NC}"
    sleep 10

    # 验证应用启动
    if curl -s http://localhost:8080/actuator/health &> /dev/null; then
        echo -e "${GREEN}应用启动成功！${NC}"
    else
        echo -e "${YELLOW}应用启动超时${NC}"
        exit 1
    fi
fi

# 生成测试数据
if [ ! -f "users_multi.csv" ] || [ ! -f "users_single.csv" ]; then
    echo -e "${GREEN}生成测试用户数据...${NC}"
    bash generate_test_users.sh
else
    echo -e "${YELLOW}测试数据文件已存在${NC}"
fi

# 初始化数据库
#echo -e "${GREEN}重置数据库...${NC}"
#mysql -u root -p"petcare" < /Users/michael/IdeaProjects/petcare/doc/performance/init_test_data.sql 2>&1 | grep -v "Warning\|Error"
#
#if [ $? -eq 0 ]; then
#    echo -e "${GREEN}数据库初始化成功${NC}"
#else
#    echo -e "${RED}数据库初始化失败${NC}"
#    exit 1
#fi

# 执行压测
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}开始压测...${NC}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULT_FILE="results/result_${TIMESTAMP}.jtl"
REPORT_DIR="results/report_${TIMESTAMP}"

jmeter -n -t points_consume_test.jmx -l "$RESULT_FILE" -e -o "$REPORT_DIR" -JBASE_URL=http://localhost:8080

JMETER_EXIT=$?

# 解析并显示测试结果
parse_jtl_results() {
    local jtl_file=$1

    if [ ! -f "$jtl_file" ]; then
        echo -e "${RED}JTL 文件不存在: $jtl_file${NC}"
        return 1
    fi

    # 读取 JTL 文件（跳过表头）
    # JTL 格式：timeStamp,elapsed,label,responseCode,responseMessage,threadName,dataType,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect
    local total_samples=$(tail -n +2 "$jtl_file" | wc -l | tr -d ' ')
    local errors=$(tail -n +2 "$jtl_file" | awk -F',' '($9 == "false") {count++} END {print count+0}')
    local success=$((total_samples - errors))
    local error_rate=0
    if [ $total_samples -gt 0 ]; then
        error_rate=$(echo "scale=2; $errors * 100 / $total_samples" | bc)
    fi

    # 响应时间统计 (第4列: elapsed，单位ms)
    local avg_time=$(tail -n +2 "$jtl_file" | awk -F',' '{sum+=$2; count++} END {if(count>0) printf "%.0f", sum/count}')
    local min_time=$(tail -n +2 "$jtl_file" | awk -F',' 'NR==1 || $2<min {min=$2} END {print min+0}')
    local max_time=$(tail -n +2 "$jtl_file" | awk -F',' 'NR==1 || $2>max {max=$2} END {print max+0}')

    # 计算 P50, P95, P99 延迟
    local p50=$(tail -n +2 "$jtl_file" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {arr[count++]=$1} END{if(count>0) printf "%.0f", arr[int(count*0.5)]}')
    local p95=$(tail -n +2 "$jtl_file" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {arr[count++]=$1} END{if(count>0) printf "%.0f", arr[int(count*0.95)]}')
    local p99=$(tail -n +2 "$jtl_file" | awk -F',' '{print $2}' | sort -n | awk 'BEGIN{count=0} {arr[count++]=$1} END{if(count>0) printf "%.0f", arr[int(count*0.99)]}')

    # 计算测试时长（秒）和 QPS
    # 跳过表头，取第一条和最后一条记录的时间戳
    local first_line=$(tail -n +2 "$jtl_file" | head -1)
    local last_line=$(tail -1 "$jtl_file")

    local start_time=$(echo "$first_line" | awk -F',' '{printf "%.0f", $1/1000}')
    local end_time=$(echo "$last_line" | awk -F',' '{printf "%.0f", $1/1000}')

    local duration=0
    local qps=0

    if [ ! -z "$start_time" ] && [ ! -z "$end_time" ] && [ "$start_time" -gt 0 ]; then
        duration=$((end_time - start_time))
        if [ "$duration" -gt 0 ]; then
            qps=$((total_samples / duration))
        fi
    fi

    # 输出结果
    echo ""
    echo -e "${GREEN}========================================"
    echo -e "${GREEN}性能测试结果汇总${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo -e "${GREEN}【请求统计】${NC}"
    echo "  总请求数:     $total_samples"
    echo "  成功请求:     $success"
    echo "  失败请求:     $errors"
    echo "  错误率:       ${error_rate}%"
    echo ""
    echo -e "${GREEN}【响应时间】${NC}"
    echo "  平均响应:     ${avg_time} ms"
    echo "  最小响应:     ${min_time} ms"
    echo "  最大响应:     ${max_time} ms"
    echo "  P50 延迟:     ${p50} ms"
    echo "  P95 延迟:     ${p95} ms"
    echo "  P99 延迟:     ${p99} ms"
    echo ""
    echo -e "${GREEN}【吞吐量】${NC}"
    echo "  测试时长:     ${duration} 秒"
    echo "  QPS:          $qps"
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}查看详细报告：${NC}"
    echo -e "  macOS: open $REPORT_DIR/index.html"
    echo -e "  Linux: xdg-open $REPORT_DIR/index.html"
    echo ""
}

if [ $JMETER_EXIT -eq 0 ]; then
    echo ""
    parse_jtl_results "$RESULT_FILE"
    JMETER_EXIT=0
else
    echo ""
    echo -e "${RED}压测失败，退出码: $?${NC}"
    echo ""
    echo -e "${RED}========================================${NC}"
    JMETER_EXIT=1
fi
