package com.example.demo.webSocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class EmaBuySellHandler extends TextWebSocketHandler {

    private static final Set<WebSocketSession> sessions =
            new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session)
            throws Exception {

        sessions.add(session);

        session.sendMessage(
                new TextMessage("EMA WebSocket Connected")
        );
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessions.remove(session);

        System.out.println("EMA Browser Disconnected");
    }

    public void sendMessage(String message) {

        for (WebSocketSession session : sessions) {

            try {

                if (session.isOpen()) {

                    session.sendMessage(
                            new TextMessage(message)
                    );
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }
}