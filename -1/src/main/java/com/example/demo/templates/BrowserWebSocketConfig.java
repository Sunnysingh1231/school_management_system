package com.example.demo.templates;


import com.example.demo.templates.MarketWebSocketHandler;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class BrowserWebSocketConfig
        implements WebSocketConfigurer {

    private final MarketWebSocketHandler handler;

    public BrowserWebSocketConfig(
            MarketWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(
            WebSocketHandlerRegistry registry) {

        registry
            .addHandler(handler, "/ws/market")
            .setAllowedOrigins("*");

        registry
            .addHandler(handler, "/ws/algo")
            .setAllowedOrigins("*");
    }
}