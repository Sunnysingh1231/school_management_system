package com.example.demo.webSocket;

import com.example.demo.model.MarketTick;
import com.example.demo.service.HistoricalLiveSimulator;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class HistoricalLiveWebSocketHandler
        extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final HistoricalLiveSimulator simulator;

    private final Set<WebSocketSession> sessions =
            new CopyOnWriteArraySet<>();

    public HistoricalLiveWebSocketHandler(
            ObjectMapper objectMapper,
            @Lazy HistoricalLiveSimulator simulator) {

        this.objectMapper = objectMapper;
        this.simulator = simulator;
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) {

        sessions.add(session);

        System.out.println(
                "HISTORICAL LIVE SOCKET CONNECTED: "
                        + session.getId()
        );

        System.out.println(
                "HISTORICAL LIVE SESSION COUNT = "
                        + sessions.size()
        );

        // Browser connect hone par current stocks bhejna
        sendAllMarketData(session);
    }

    /**
     * Simulator se har 5 second call hoga.
     */
    public void sendMarketData(MarketTick tick) {

        if (tick == null) {
            return;
        }

        try {

            String json =
                    objectMapper.writeValueAsString(tick);

            broadcast(json);

        } catch (Exception e) {

            System.err.println(
                    "HISTORICAL LIVE JSON ERROR"
            );

            e.printStackTrace();
        }
    }

    /**
     * Browser connect hone par current stocks bhejna.
     */
    private void sendAllMarketData(
            WebSocketSession session) {

        try {

            Map<String, MarketTick> stocks =
                    simulator.getLiveStocks();

            String json =
                    objectMapper.writeValueAsString(stocks);

            sendToSession(session, json);

            System.out.println(
                    "INITIAL HISTORICAL LIVE DATA SENT"
            );

        } catch (Exception e) {

            System.err.println(
                    "INITIAL HISTORICAL LIVE SEND ERROR"
            );

            e.printStackTrace();
        }
    }

    /**
     * Single session ko data send karta hai.
     */
    private void sendToSession(
            WebSocketSession session,
            String json) throws IOException {

        if (!session.isOpen()) {
            return;
        }

        synchronized (session) {

            if (session.isOpen()) {

                session.sendMessage(
                        new TextMessage(json)
                );
            }
        }
    }

    /**
     * Sabhi connected browser sessions ko data bhejna.
     */
    private void broadcast(String json) {

        for (WebSocketSession session : sessions) {

            if (!session.isOpen()) {

                sessions.remove(session);

                continue;
            }

            try {

                synchronized (session) {

                    if (session.isOpen()) {

                        session.sendMessage(
                                new TextMessage(json)
                        );
                    }
                }

            } catch (Exception e) {

                sessions.remove(session);

                System.err.println(
                        "HISTORICAL LIVE SOCKET SEND FAILED: "
                                + session.getId()
                );
            }
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessions.remove(session);

        System.out.println(
                "HISTORICAL LIVE SOCKET CLOSED = "
                        + session.getId()
        );
    }

    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception) {

        sessions.remove(session);

        System.err.println(
                "HISTORICAL LIVE SOCKET ERROR = "
                        + session.getId()
        );

        exception.printStackTrace();
    }
}