package com.example.demo;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class LiveMarketWebSocketHandler
        implements WebSocketHandler {

    private final LivePriceService livePriceService;


    public LiveMarketWebSocketHandler(
            LivePriceService livePriceService) {

        this.livePriceService =
                livePriceService;
    }


    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) {

        System.out.println(
                "Browser connected: "
                        + session.getId()
        );

        livePriceService.addSession(session);
    }


    @Override
    public void handleMessage(
            WebSocketSession session,
            WebSocketMessage<?> message) {

        // Browser → server messages
        // currently not required
    }


    @Override
    public void handleTransportError(
            WebSocketSession session,
            Throwable exception) {

        exception.printStackTrace();
    }


    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus closeStatus) {

        livePriceService.removeSession(session);

        System.out.println(
                "Browser disconnected"
        );
    }


    @Override
    public boolean supportsPartialMessages() {

        return false;
    }
}