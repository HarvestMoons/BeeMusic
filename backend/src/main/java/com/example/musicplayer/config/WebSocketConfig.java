package com.example.musicplayer.config;

import com.example.musicplayer.handler.OnlineCountHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OnlineCountHandler onlineCountHandler;
    private final String allowedOrigins;

    public WebSocketConfig(
            OnlineCountHandler onlineCountHandler,
            @Value("${app.websocket.allowed-origins:https://beemusic.fun,https://www.beemusic.fun,http://localhost,https://localhost,http://localhost:5173,http://localhost:8081}") String allowedOrigins) {
        this.onlineCountHandler = onlineCountHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(onlineCountHandler, "/ws/online")
                .setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toArray(String[]::new));
        //不要使用 .withSockJS(); 否则会导致连接失败
    }
}