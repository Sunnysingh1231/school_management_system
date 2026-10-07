
package com.example.demo.webSocket;

import com.example.demo.model.MarketTick;
import com.example.demo.service.LiquidityMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class LiquidityWebSocketHandler extends TextWebSocketHandler {

	private final ObjectMapper objectMapper;
	private final LiquidityMetricsService liquidityMetricsService;

	private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

	public LiquidityWebSocketHandler(ObjectMapper objectMapper, LiquidityMetricsService liquidityMetricsService) {

		this.objectMapper = objectMapper;
		this.liquidityMetricsService = liquidityMetricsService;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {

		sessions.add(session);

		System.out.println("LIQUIDITY SOCKET CONNECTED: " + session.getId());

		System.out.println("LIQUIDITY SESSION COUNT = " + sessions.size());

		sendLiquidityToSession(session);
	}

	/**
	 * Called by MarketWebSocketHandler for every live MarketTick.
	 */
	public void updateFromMarketTick(MarketTick tick) {

		if (tick == null) {
			return;
		}

		try {

			// Update liquidity calculations
			liquidityMetricsService.update(tick);
			
//			System.out.println(tick);

			// Get latest liquidity stocks
			String json = objectMapper.writeValueAsString(liquidityMetricsService.getLiquidityStocks());

			// Send to all liquidity clients
			broadcast(json);

		} catch (Exception e) {

			System.err.println("LIQUIDITY UPDATE ERROR");

			e.printStackTrace();
		}
	}

	/**
	 * Send current liquidity data when browser connects.
	 */
	private void sendLiquidityToSession(WebSocketSession session) {

		try {

			String json = objectMapper.writeValueAsString(liquidityMetricsService.getLiquidityStocks());

			sendToSession(session, json);

			System.out.println("INITIAL LIQUIDITY DATA SENT");

		} catch (Exception e) {

			System.err.println("LIQUIDITY INITIAL SEND ERROR");

			e.printStackTrace();
		}
	}

	private void sendToSession(WebSocketSession session, String json) throws IOException {

		if (!session.isOpen()) {
			return;
		}

		synchronized (session) {

			if (session.isOpen()) {

				session.sendMessage(new TextMessage(json));
			}
		}
	}

	private void broadcast(String json) {

		for (WebSocketSession session : sessions) {

			if (!session.isOpen()) {

				sessions.remove(session);

				continue;
			}

			try {

				synchronized (session) {

					if (session.isOpen()) {

						session.sendMessage(new TextMessage(json));
					}
				}

			} catch (Exception e) {

				sessions.remove(session);

				System.err.println("LIQUIDITY SOCKET SEND FAILED: " + session.getId());
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

		sessions.remove(session);

		System.out.println("LIQUIDITY SOCKET CLOSED = " + session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {

		sessions.remove(session);

		System.err.println("LIQUIDITY SOCKET ERROR = " + session.getId());

		exception.printStackTrace();
	}
}