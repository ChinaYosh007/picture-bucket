local capacity = tonumber(ARGV[1])
local refillPeriodMillis = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local tokens = tonumber(redis.call('HGET', KEYS[1], 'tokens'))
local lastRefill = tonumber(redis.call('HGET', KEYS[1], 'lastRefill'))

if tokens == nil then
    tokens = capacity
    lastRefill = now
end

local elapsed = now - lastRefill
if elapsed > 0 then
    tokens = math.min(capacity, tokens + elapsed * capacity / refillPeriodMillis)
    lastRefill = now
end

if tokens < 1 then
    redis.call('HSET', KEYS[1], 'tokens', tokens, 'lastRefill', lastRefill)
    redis.call('PEXPIRE', KEYS[1], refillPeriodMillis * 2)
    return 0
end

redis.call('HSET', KEYS[1], 'tokens', tokens - 1, 'lastRefill', lastRefill)
redis.call('PEXPIRE', KEYS[1], refillPeriodMillis * 2)
return 1
