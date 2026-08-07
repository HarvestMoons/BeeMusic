package com.example.musicplayer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoteRedisProjector {
    private static final DefaultRedisScript<Long> PROJECT_SCRIPT = new DefaultRedisScript<>(
            "if ARGV[1] == '1' then " +
                    "redis.call('sadd', KEYS[1], ARGV[2]); " +
                    "redis.call('srem', KEYS[2], ARGV[2]); " +
                    "elseif ARGV[1] == '-1' then " +
                    "redis.call('srem', KEYS[1], ARGV[2]); " +
                    "redis.call('sadd', KEYS[2], ARGV[2]); " +
                    "else " +
                    "redis.call('srem', KEYS[1], ARGV[2]); " +
                    "redis.call('srem', KEYS[2], ARGV[2]); " +
                    "end; return 1",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public void project(Long songId, Long userId, Integer voteCode) {
        String vote = voteCode == null ? "0" : voteCode.toString();
        redisTemplate.execute(
                PROJECT_SCRIPT,
                java.util.List.of("likes:" + songId, "dislikes:" + songId),
                vote,
                userId.toString());
    }
}
