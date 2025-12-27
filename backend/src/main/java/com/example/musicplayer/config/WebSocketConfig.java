package com.example.musicplayer.config;

import com.example.musicplayer.handler.OnlineCountHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OnlineCountHandler onlineCountHandler;

    public WebSocketConfig(OnlineCountHandler onlineCountHandler) {
        this.onlineCountHandler = onlineCountHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(onlineCountHandler, "/ws/online")
                .setAllowedOriginPatterns("*");// 生产环境改成你的域名
        //不要使用 .withSockJS(); 否则会导致连接失败
    }
}