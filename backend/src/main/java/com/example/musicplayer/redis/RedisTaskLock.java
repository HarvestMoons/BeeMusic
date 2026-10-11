package com.example.musicplayer.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 基于 Redis 的多实例定时任务租约锁。
 * 获取成功返回 token，释放时用 Lua 脚本比对 token，避免误删其他实例的锁。
 */
@Component
@RequiredArgsConstructor
public class RedisTaskLock {

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public String tryAcquire(String key, Duration leaseTime) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, token, leaseTime);
        return Boolean.TRUE.equals(acquired) ? token : null;
    }

    public void release(String key, String token) {
        if (token != null) {
            redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(key), token);
        }
    }
}
