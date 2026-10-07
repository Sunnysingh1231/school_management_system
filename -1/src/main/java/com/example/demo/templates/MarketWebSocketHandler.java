package com.example.demo.templates;

import com.example.demo.model.FilteredStock;
import com.example.demo.model.MarketTick;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class MarketWebSocketHandler
        extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    private final Set<WebSocketSession> marketSessions =
            new CopyOnWriteArraySet<>();

    private final Set<WebSocketSession> algoSessions =
            new CopyOnWriteArraySet<>();

    public MarketWebSocketHandler(
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    // =========================================================
    // CONNECTION
    // =========================================================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String path = session.getUri() != null
                ? session.getUri().getPath()
                : "";

        if ("/ws/algo".equals(path)) {

            algoSessions.add(session);

            System.out.println(
                    "ALGO CLIENT CONNECTED = " + session.getId()
            );

        } else if ("/ws/market".equals(path)) {

            marketSessions.add(session);

            System.out.println(
                    "MARKET CLIENT CONNECTED = " + session.getId()
            );
        }
    }

    // =========================================================
    // MARKET DATA
    // =========================================================

    public void sendMarketData(
            MarketTick tick) {
    	
        try {

            String json =
                    objectMapper.writeValueAsString(
                            tick
                    );

            for (WebSocketSession session :
                    marketSessions) {

                if (!session.isOpen()) {
                    continue;
                }

                synchronized (session) {

                    session.sendMessage(
                            new TextMessage(json)
                    );
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "MARKET WEBSOCKET SEND ERROR"
            );

            e.printStackTrace();
        }
    }

    // ALGO DATA
    public void sendAlgoData(
            FilteredStock stock) {

        try {

            String json =
                    objectMapper.writeValueAsString(
                            stock
                    );

            for (WebSocketSession session :
                    algoSessions) {

                if (!session.isOpen()) {
                    continue;
                }

                synchronized (session) {

                    session.sendMessage(
                            new TextMessage(json)
                    );
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "ALGO WEBSOCKET SEND ERROR"
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // DISCONNECT
    // =========================================================

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            org.springframework.web.socket.CloseStatus status) {

        marketSessions.remove(session);
        algoSessions.remove(session);

        System.out.println(
                "WEBSOCKET CLOSED = "
                        + session.getId()
        );
    }

    // =========================================================
    // ERROR
    // =========================================================

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception) {

        marketSessions.remove(session);
        algoSessions.remove(session);

        System.err.println(
                "WEBSOCKET ERROR = "
                        + session.getId()
        );
    }
}