-- 令牌桶限流（原子执行：补充令牌 + 扣减 + 返回是否放行）
-- KEYS[1] = 限流 key
-- ARGV[1] = 桶容量 capacity
-- ARGV[2] = 每秒补充速率 refillRate
-- ARGV[3] = 当前时间（秒）
-- ARGV[4] = 本次消耗令牌数（默认 1）
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4]) or 1

local data = redis.call('HMGET', key, 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts = tonumber(data[2])
if tokens == nil then
    tokens = capacity
end
if ts == nil then
    ts = now
end

-- 按时间补充令牌
local delta = math.max(0, now - ts)
tokens = math.min(capacity, tokens + delta * refillRate)

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
redis.call('EXPIRE', key, 300)
return allowed
