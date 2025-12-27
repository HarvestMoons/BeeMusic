// src/main/java/com/example/musicplayer/handler/OnlineCountHandler.java
package com.example.musicplayer.handler;

import com.example.musicplayer.listener.OnlineCountListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class OnlineCountHandler extends TextWebSocketHandler {

    private final StringRedisTemplate redisTemplate;
    private final OnlineCountListener redisListener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ONLINE_TOTAL_KEY = "music:online:total";
    private static final String SONG_LISTENERS_HASH = "music:online:song:listeners";
    private static final String REDIS_CHANNEL = "online-count-channel";

    // 关键：用 session 属性防止重复计数
    private static final String COUNTED_FLAG = "online_counted";
    private static final String CURRENT_SONG_ID = "songId";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        redisListener.addSession(session);

        // 防止页面刷新重复 +1
        AtomicBoolean counted = (AtomicBoolean) session.getAttributes()
                .computeIfAbsent(COUNTED_FLAG, _ -> new AtomicBoolean(false));

        if (counted.compareAndSet(false, true)) {
            redisTemplate.opsForValue().increment(ONLINE_TOTAL_KEY);
        }

        broadcast();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        redisListener.removeSession(session);

        String oldSongId = (String) session.getAttributes().get(CURRENT_SONG_ID);
        if (oldSongId != null) {
            Long result = redisTemplate.opsForHash().increment(SONG_LISTENERS_HASH, oldSongId, -1L);
            // 防止负数（极端情况：Redis 重启或清理后旧数据残留）
            if (result < 0) {
                redisTemplate.opsForHash().put(SONG_LISTENERS_HASH, oldSongId, "0");
            }
        }

        // 只在真正计数过的情况下才减1
        AtomicBoolean counted = (AtomicBoolean) session.getAttributes().get(COUNTED_FLAG);
        if (counted != null && counted.get()) {
            redisTemplate.opsForValue().decrement(ONLINE_TOTAL_KEY);
        }

        broadcast();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> jsonMap = objectMapper.readValue(message.getPayload(), Map.class);
        System.out.println("Received WebSocket message: " + jsonMap);

        // 兼容前端可能传数字的情况
        Object songIdObj = jsonMap.get("songId");
        String songId = songIdObj == null ? null : String.valueOf(songIdObj);

        String oldSongId = (String) session.getAttributes().get(CURRENT_SONG_ID);

        if (!Objects.equals(oldSongId, songId)) {
            // 减旧
            if (oldSongId != null) {
                Long result = redisTemplate.opsForHash().increment(SONG_LISTENERS_HASH, oldSongId, -1L);
                if (result < 0) {
                    redisTemplate.opsForHash().put(SONG_LISTENERS_HASH, oldSongId, "0");
                }
            }
            // 加新
            if (songId != null) {
                redisTemplate.opsForHash().increment(SONG_LISTENERS_HASH, songId, 1L);
            }

            session.getAttributes().put(CURRENT_SONG_ID, songId);
            broadcast();
        }
    }

    private void broadcast() {
        Long total = redisTemplate.opsForValue().increment(ONLINE_TOTAL_KEY, 0L);
        if (total == null || total < 0) {
            total = 0L;
            redisTemplate.opsForValue().set(ONLINE_TOTAL_KEY, "0");
        }

        Map<Object, Object> songMap = redisTemplate.opsForHash().entries(SONG_LISTENERS_HASH);

        Map<String, Object> data = Map.of(
                "onlineCount", total,
                "songListeners", songMap
        );

        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.convertAndSend(REDIS_CHANNEL, json);
        } catch (Exception ignored) {
        }
    }
}