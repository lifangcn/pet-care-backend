-- wrk2 压测脚本 - 单用户消耗积分
-- 验证分布式锁串行化影响
-- 创建时间: 2025-02-21
-- 作者: Michael Li

wrk.method = "POST"
wrk.body   = '{"userId": 1001, "points": -10, "actionType": "AI_CONSULT"}'
wrk.headers["Content-Type"] = "application/json"
wrk.headers["X-Test-User-Id"] = "1001"

-- 响应处理
response = function(status, headers, body)
  if status ~= 200 then
    print("Error: " .. status .. " - " .. body)
  end
end
