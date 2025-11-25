package com.example.musicplayer.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class OnlineCountListener implements MessageListener {

    // 每个实例维护自己的 sessions（不共享，不 static）
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 让 OnlineCountHandler 能把新连接塞进来
    public void addSession(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session.getId());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String jsonMessage = new String(message.getBody());

        // 清理失效会话 + 广播
        sessions.values().removeIf(s -> !s.isOpen());

        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
            } catch (IOException ignored) {}
        }
    }
}