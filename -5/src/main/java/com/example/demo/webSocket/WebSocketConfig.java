package com.example.demo.webSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

		registry.addHandler(new WebSocketHandler(), "/ws/test").setAllowedOrigins("*");
		
		registry.addHandler(new EmaWebSocketHandler(), "/ws/ema").setAllowedOrigins("*");
		
		registry.addHandler(new EmaBuySellHandler(), "/ws/bs").setAllowedOrigins("*");
		
	}
}
