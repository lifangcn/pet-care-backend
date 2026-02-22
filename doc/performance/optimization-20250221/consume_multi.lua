-- wrk2 压测脚本 - 多用户消耗积分
-- 验证系统真实吞吐能力
-- 创建时间: 2025-02-21
-- 作者: Michael Li

wrk.method = "POST"
wrk.headers["Content-Type"] = "application/json"

-- 1000 个测试用户 (userId: 1-1000)
local user_ids = {}
for i = 1, 1000 do
  table.insert(user_ids, i)
end

-- 随机选择用户
local index = 0
local math_random = math.random
local table_insert = table.insert

request = function()
  -- 循环使用用户 ID
  index = index % 1000 + 1
  local user_id = user_ids[index]

  -- 构造请求体
  local body = string.format(
    '{"userId": %d, "points": -10, "actionType": "AI_CONSULT"}',
    user_id
  )

  -- 设置测试用户头
  wrk.headers["X-Test-User-Id"] = tostring(user_id)

  return wrk.format(nil, nil, nil, body)
end

-- 响应处理
response = function(status, headers, body)
  if status ~= 200 then
    print("Error: " .. status)
  end
end
