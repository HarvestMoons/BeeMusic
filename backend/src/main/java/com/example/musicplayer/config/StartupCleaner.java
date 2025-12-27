package com.example.musicplayer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
//@Profile("dev")   // 开启后 只有 dev 环境才生效
@RequiredArgsConstructor
public class StartupCleaner {

    private final StringRedisTemplate redisTemplate;

    private static final String ONLINE_TOTAL_KEY = "music:online:total";
    private static final String SONG_LISTENERS_HASH = "music:online:song:listeners";

    @EventListener(ApplicationReadyEvent.class)
    public void clean() {
        redisTemplate.delete(ONLINE_TOTAL_KEY);
        redisTemplate.delete(SONG_LISTENERS_HASH);
        System.out.println("【开发环境】在线人数和同曲听众计数已自动重置为 0");
    }
}