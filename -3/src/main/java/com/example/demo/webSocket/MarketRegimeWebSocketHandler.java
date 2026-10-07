
package com.example.demo.webSocket;

import com.example.demo.model.MarketTick;
import com.example.demo.service.MarketRegimeService;
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
public class MarketRegimeWebSocketHandler extends TextWebSocketHandler {

	private final ObjectMapper objectMapper;
	private final MarketRegimeService marketRegimeService;

	private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

	public MarketRegimeWebSocketHandler(ObjectMapper objectMapper, MarketRegimeService marketRegimeService) {

		this.objectMapper = objectMapper;
		this.marketRegimeService = marketRegimeService;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {

		sessions.add(session);

		System.out.println("MARKET REGIME SOCKET CONNECTED: " + session.getId());

		System.out.println("MARKET REGIME SESSION COUNT = " + sessions.size());

		sendMarketRegimeToSession(session);
	}

	/**
	 * Called by MarketWebSocketHandler for every live MarketTick.
	 */
	public void updateFromMarketTick(MarketTick tick) {

		if (tick == null) {
			return;
		}

		try {

			// Update EMA/regime calculation
			marketRegimeService.update(tick);

			// Get latest filtered stocks
			String json = objectMapper.writeValueAsString(marketRegimeService.getFilteredStocks());

			// Send to all regime clients
			broadcast(json);

		} catch (Exception e) {

			System.err.println("MARKET REGIME UPDATE ERROR");

			e.printStackTrace();
		}
	}

	/**
	 * Send current regime data when browser connects.
	 */
	private void sendMarketRegimeToSession(WebSocketSession session) {

		try {

			String json = objectMapper.writeValueAsString(marketRegimeService.getFilteredStocks());

			sendToSession(session, json);

			System.out.println("INITIAL MARKET REGIME SENT");

		} catch (Exception e) {

			System.err.println("REGIME INITIAL SEND ERROR");

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

				System.err.println("MARKET REGIME SOCKET SEND FAILED: " + session.getId());
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

		sessions.remove(session);

		System.out.println("MARKET REGIME SOCKET CLOSED = " + session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {

		sessions.remove(session);

		System.err.println("MARKET REGIME SOCKET ERROR = " + session.getId());

		exception.printStackTrace();
	}
}