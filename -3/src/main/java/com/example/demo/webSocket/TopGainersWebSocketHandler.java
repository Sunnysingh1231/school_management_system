
package com.example.demo.webSocket;

import com.example.demo.model.MarketTick;
import com.example.demo.service.TopGainersService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class TopGainersWebSocketHandler extends TextWebSocketHandler {

	private final ObjectMapper objectMapper;
	private final TopGainersService topGainersService;

	private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

	public TopGainersWebSocketHandler(ObjectMapper objectMapper, TopGainersService topGainersService) {

		this.objectMapper = objectMapper;
		this.topGainersService = topGainersService;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {

		sessions.add(session);

		System.out.println("TOP GAINERS SOCKET CONNECTED: " + session.getId());

		System.out.println("TOP GAINERS SESSION COUNT = " + sessions.size());

		sendTopGainersToSession(session);
	}

	/**
	 * Called by MarketWebSocketHandler whenever a new MarketTick arrives.
	 */
	public void updateFromMarketTick(MarketTick tick) {

		if (tick == null) {
			return;
		}

		try {

			/*
			 * IMPORTANT: Tumhare original code me ye line commented thi. Isliye same
			 * behavior maintain karne ke liye abhi commented hi rakhi hai.
			 *
			 * Agar TopGainersService ko har live tick manually update karna hai to isko
			 * uncomment kar sakte ho.
			 */

			// topGainersService.update(tick);

			List<MarketTick> topGainers = topGainersService.getTopGainers();

			String json = objectMapper.writeValueAsString(topGainers);

			broadcast(json);

		} catch (Exception e) {

			System.err.println("TOP GAINERS UPDATE ERROR");

			e.printStackTrace();
		}
	}

	/**
	 * Send initial Top Gainers list when browser connects.
	 */
	private void sendTopGainersToSession(WebSocketSession session) {

		try {

			List<MarketTick> topGainers = topGainersService.getTopGainers();

			String json = objectMapper.writeValueAsString(topGainers);

			sendToSession(session, json);

			System.out.println("INITIAL TOP GAINERS SENT = " + topGainers.size());

		} catch (Exception e) {

			System.err.println("ERROR SENDING INITIAL TOP GAINERS");

			e.printStackTrace();
		}
	}

	/**
	 * Optional external broadcast method.
	 */
	public void sendTopGainers(List<MarketTick> topGainers) {

		try {

			if (topGainers == null) {
				return;
			}

			String json = objectMapper.writeValueAsString(topGainers);

			broadcast(json);

		} catch (Exception e) {

			System.err.println("TOP GAINERS BROADCAST ERROR");

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

				System.err.println("TOP GAINERS SOCKET SEND FAILED: " + session.getId());
			}
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

		sessions.remove(session);

		System.out.println("TOP GAINERS SOCKET CLOSED = " + session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {

		sessions.remove(session);

		System.err.println("TOP GAINERS SOCKET ERROR = " + session.getId());

		exception.printStackTrace();
	}
}