
package com.example.demo.webSocket;

import com.example.demo.model.MarketTick;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class MarketWebSocketHandler extends TextWebSocketHandler {

	private final ObjectMapper objectMapper;

	// Other WebSocket handlers
	private final LiquidityWebSocketHandler liquidityWebSocketHandler;

	// Normal market browser sessions
	private final Set<WebSocketSession> marketSessions = new CopyOnWriteArraySet<>();

	// Latest market data cache
	private final Map<String, MarketTick> latestMarketData = new ConcurrentHashMap<>();

	public MarketWebSocketHandler(ObjectMapper objectMapper,
			
			LiquidityWebSocketHandler liquidityWebSocketHandler) {

		this.objectMapper = objectMapper;
		this.liquidityWebSocketHandler = liquidityWebSocketHandler;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {

		marketSessions.add(session);

		System.out.println("MARKET SOCKET CONNECTED: " + session.getId());

		System.out.println("MARKET SESSION COUNT = " + marketSessions.size());

		sendLatestMarketData(session);
	}

	/**
	 * Called whenever a new MarketTick arrives from Upstox.
	 *
	 * IMPORTANT: Upstox service can continue calling this same method.
	 */
	public void sendMarketData(MarketTick tick) {

		if (tick == null || tick.getInstrumentKey() == null) {
			return;
		}

		try {

			// =====================================================
			// 1. CACHE LATEST MARKET DATA
			// =====================================================

			latestMarketData.put(tick.getInstrumentKey(), tick);

			// =====================================================
			// 2. SEND NORMAL MARKET DATA
			// =====================================================

			String marketJson = objectMapper.writeValueAsString(tick);

			broadcast(marketSessions, marketJson);

			// =====================================================
			// 5. SEND TICK TO LIQUIDITY
			// =====================================================

			liquidityWebSocketHandler.updateFromMarketTick(tick);

		} catch (Exception e) {

			System.err.println("LIVE MARKET UPDATE ERROR");

			e.printStackTrace();
		}
	}

	/**
	 * Send cached market data when browser connects/reconnects.
	 */
	private void sendLatestMarketData(WebSocketSession session) {

		for (MarketTick tick : latestMarketData.values()) {

			if (!session.isOpen()) {
				return;
			}

			try {

				String json = objectMapper.writeValueAsString(tick);

				sendToSession(session, json);

			} catch (Exception e) {

				System.err.println("FAILED TO SEND CACHED MARKET DATA");

				e.printStackTrace();
			}
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

	private void broadcast(Set<WebSocketSession> sessions, String json) {

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

				System.err.println("WEBSOCKET SEND FAILED: " + session.getId());

				e.printStackTrace();
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

		marketSessions.remove(session);

		System.out.println("MARKET SOCKET CLOSED = " + session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {

		marketSessions.remove(session);

		System.err.println("MARKET WEBSOCKET ERROR = " + session.getId());

		exception.printStackTrace();
	}
}
