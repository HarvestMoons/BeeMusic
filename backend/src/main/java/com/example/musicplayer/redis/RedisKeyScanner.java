package com.example.musicplayer.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 按模式用 SCAN 游标批量删除 Redis 键。
 * 生产环境不应使用 KEYS 命令；把这段游标逻辑集中一处，避免多处重复实现导致行为不一致。
 */
@Component
@RequiredArgsConstructor
public class RedisKeyScanner {

    private static final long SCAN_COUNT = 500;

    private final StringRedisTemplate redisTemplate;

    /**
     * 删除所有匹配 {@code pattern} 的键。
     *
     * @return 实际删除的键数量
     */
    public int deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new HashSet<>();
            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            try (var cursor = connection.scan(ScanOptions.scanOptions()
                    .match(pattern).count(SCAN_COUNT).build())) {
                while (cursor.hasNext()) {
                    result.add(serializer.deserialize(cursor.next()));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to scan Redis keys: " + pattern, e);
            }
            return result;
        });

        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        redisTemplate.delete(keys);
        return keys.size();
    }
}
