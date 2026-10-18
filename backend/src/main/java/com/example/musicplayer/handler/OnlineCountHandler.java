package com.example.musicplayer.handler;

import com.example.musicplayer.listener.OnlineCountListener;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OnlineCountHandler extends TextWebSocketHandler {
    private final StringRedisTemplate redisTemplate;
    private final OnlineCountListener redisListener;
    private final ObjectMapper objectMapper;

    private static final String ONLINE_TOTAL_KEY = "music:online:sessions";
    private static final String ONLINE_SONGS_KEY = "music:online:songs";
    private static final String SONG_PREFIX = "music:online:song:";
    private static final String REDIS_CHANNEL = "online-count-channel";
    private static final long SESSION_TTL_MILLIS = 90_000L;
    private static final String CLIENT_ID = "onlineClientId";

    // 一个页面只占一个成员；旧连接的心跳或关闭事件不能覆盖、删除新连接。
    private static final DefaultRedisScript<Long> UPDATE_PRESENCE = new DefaultRedisScript<>("""
            local owner = redis.call('HGET', KEYS[1], 'owner')
            if ARGV[1] ~= 'connect' and owner ~= ARGV[2] then return 0 end
            local oldSong = redis.call('HGET', KEYS[1], 'song')
            if oldSong and oldSong ~= '' then
                redis.call('ZREM', ARGV[6] .. oldSong, ARGV[3])
            end
            if ARGV[1] == 'close' then
                redis.call('ZREM', KEYS[2], ARGV[3])
                redis.call('DEL', KEYS[1])
            else
                redis.call('HSET', KEYS[1], 'owner', ARGV[2], 'song', ARGV[4])
                redis.call('PEXPIRE', KEYS[1], ARGV[7])
                redis.call('ZADD', KEYS[2], ARGV[5], ARGV[3])
                if ARGV[4] ~= '' then
                    redis.call('SADD', KEYS[3], ARGV[4])
                    redis.call('ZADD', ARGV[6] .. ARGV[4], ARGV[5], ARGV[3])
                end
            end
            return 1
            """, Long.class);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String clientId = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst("clientId");
        try {
            clientId = UUID.fromString(clientId).toString();
        } catch (IllegalArgumentException | NullPointerException error) {
            clientId = session.getId(); // 兼容尚未刷新的旧客户端。
        }
        session.getAttributes().put(CLIENT_ID, clientId);
        updatePresence(session, "connect", "");
        redisListener.addSession(session);
        broadcast();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        redisListener.removeSession(session);
        if (session.getAttributes().containsKey(CLIENT_ID)) {
            updatePresence(session, "close", "");
            broadcast();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        try {
            if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
        } finally {
            afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> data = objectMapper.readValue(message.getPayload(), new TypeReference<>() {});
        Object value = data.get("songId");
        String songId = value == null ? "" : String.valueOf(value).trim();
        if (!songId.isEmpty() && !songId.matches("[0-9]{1,19}")) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        if (!updatePresence(session, "heartbeat", songId)) {
            session.close(CloseStatus.NORMAL);
            return;
        }
        broadcast();
    }

    private boolean updatePresence(WebSocketSession session, String operation, String songId) {
        String clientId = (String) session.getAttributes().get(CLIENT_ID);
        Long updated = redisTemplate.execute(UPDATE_PRESENCE,
                List.of("music:online:client:" + clientId, ONLINE_TOTAL_KEY, ONLINE_SONGS_KEY),
                operation, session.getId(), clientId, songId,
                String.valueOf(System.currentTimeMillis() + SESSION_TTL_MILLIS),
                SONG_PREFIX, String.valueOf(SESSION_TTL_MILLIS));
        return Long.valueOf(1).equals(updated);
    }

    private void broadcast() {
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_TOTAL_KEY, 0, now);
        Long total = redisTemplate.opsForZSet().size(ONLINE_TOTAL_KEY);
        Map<String, Long> songMap = new HashMap<>();
        Set<String> songs = redisTemplate.opsForSet().members(ONLINE_SONGS_KEY);
        if (songs != null) {
            for (String songId : songs) {
                String key = SONG_PREFIX + songId;
                redisTemplate.opsForZSet().removeRangeByScore(key, 0, now);
                Long count = redisTemplate.opsForZSet().size(key);
                if (count != null && count > 0) songMap.put(songId, count);
                // 不删除空 key：避免清理与其他实例的心跳竞争，误删刚加入的成员。
            }
        }
        try {
            redisTemplate.convertAndSend(REDIS_CHANNEL, objectMapper.writeValueAsString(Map.of(
                    "onlineCount", total == null ? 0L : total, "songListeners", songMap)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("在线人数序列化失败", error);
        }
    }
}
