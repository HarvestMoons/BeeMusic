package com.example.musicplayer.handler;

import com.example.musicplayer.listener.OnlineCountListener;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class OnlineCountHandler extends TextWebSocketHandler {

    private final StringRedisTemplate redisTemplate;
    private final OnlineCountListener redisListener;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ONLINE_TOTAL_KEY = "music:online:sessions";
    private static final String ONLINE_SONGS_KEY = "music:online:songs";
    private static final String REDIS_CHANNEL = "online-count-channel";
    private static final long SESSION_TTL_MILLIS = 90_000L;

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
            refreshSession(session);
        }

        broadcast();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        redisListener.removeSession(session);

        String oldSongId = (String) session.getAttributes().get(CURRENT_SONG_ID);
        if (oldSongId != null) {
            removeFromSong(session.getId(), oldSongId);
        }

        // 只在真正计数过的情况下才减1
        AtomicBoolean counted = (AtomicBoolean) session.getAttributes().get(COUNTED_FLAG);
        if (counted != null && counted.get()) {
            redisTemplate.opsForZSet().remove(ONLINE_TOTAL_KEY, session.getId());
        }

        broadcast();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> jsonMap = objectMapper.readValue(message.getPayload(),
                new TypeReference<Map<String, Object>>() {
                });

        // 兼容前端可能传数字的情况
        Object songIdObj = jsonMap.get("songId");
        String songId = songIdObj == null ? null : String.valueOf(songIdObj);
        if (songId != null && songId.isBlank()) {
            songId = null;
        }

        String oldSongId = (String) session.getAttributes().get(CURRENT_SONG_ID);
        refreshSession(session);

        if (!Objects.equals(oldSongId, songId)) {
            // 减旧
            if (oldSongId != null) {
                removeFromSong(session.getId(), oldSongId);
            }
            // 加新
            if (songId != null) {
                addToSong(session.getId(), songId);
            }

            session.getAttributes().put(CURRENT_SONG_ID, songId);
            broadcast();
        }
    }

    private void broadcast() {
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_TOTAL_KEY, 0, now);
        Long total = redisTemplate.opsForZSet().size(ONLINE_TOTAL_KEY);
        if (total == null) {
            total = 0L;
        }

        Map<String, Long> songMap = new java.util.HashMap<>();
        Set<String> songs = redisTemplate.opsForSet().members(ONLINE_SONGS_KEY);
        if (songs != null) {
            for (String songId : songs) {
                String key = songKey(songId);
                redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);
                Long count = redisTemplate.opsForZSet().size(key);
                if (count == null || count == 0) {
                    redisTemplate.opsForSet().remove(ONLINE_SONGS_KEY, songId);
                    redisTemplate.delete(key);
                } else {
                    songMap.put(songId, count);
                }
            }
        }

        Map<String, Object> data = Map.of(
                "onlineCount", total,
                "songListeners", songMap);

        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.convertAndSend(REDIS_CHANNEL, json);
        } catch (Exception ignored) {
        }
    }

    private void refreshSession(WebSocketSession session) {
        redisTemplate.opsForZSet().add(
                ONLINE_TOTAL_KEY,
                session.getId(),
                System.currentTimeMillis() + SESSION_TTL_MILLIS);
    }

    private void addToSong(String sessionId, String songId) {
        redisTemplate.opsForSet().add(ONLINE_SONGS_KEY, songId);
        redisTemplate.opsForZSet().add(
                songKey(songId),
                sessionId,
                System.currentTimeMillis() + SESSION_TTL_MILLIS);
    }

    private void removeFromSong(String sessionId, String songId) {
        redisTemplate.opsForZSet().remove(songKey(songId), sessionId);
    }

    private String songKey(String songId) {
        return "music:online:song:" + songId;
    }
}