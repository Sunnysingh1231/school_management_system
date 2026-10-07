
package com.example.demo.webSocket;

import com.example.demo.webSocket.LiquidityWebSocketHandler;
import com.example.demo.webSocket.MarketWebSocketHandler;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	private final MarketWebSocketHandler marketWebSocketHandler;
	private final LiquidityWebSocketHandler liquidityWebSocketHandler;
	private final HistoricalLiveWebSocketHandler historicalLiveWebSocketHandler;

	public WebSocketConfig(MarketWebSocketHandler marketWebSocketHandler,
			LiquidityWebSocketHandler liquidityWebSocketHandler,
			HistoricalLiveWebSocketHandler historicalLiveWebSocketHandler) {

		this.marketWebSocketHandler = marketWebSocketHandler;



		this.liquidityWebSocketHandler = liquidityWebSocketHandler;
		this.historicalLiveWebSocketHandler = historicalLiveWebSocketHandler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

		// =====================================================
		// NORMAL LIVE MARKET
		// =====================================================

		registry.addHandler(marketWebSocketHandler, "/ws/market").setAllowedOrigins("*");

		// =====================================================
		// TOP GAINERS
		// =====================================================


		// =====================================================
		// MARKET REGIME
		// =====================================================


		// =====================================================
		// LIQUIDITY METRICS
		// =====================================================

		registry.addHandler(liquidityWebSocketHandler, "/ws/liquidity-metrics").setAllowedOrigins("*");

		registry.addHandler(historicalLiveWebSocketHandler, "/ws/historical-live").setAllowedOrigins("*");
	}
}
